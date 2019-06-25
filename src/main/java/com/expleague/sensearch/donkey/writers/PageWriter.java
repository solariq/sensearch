package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.donkey.utils.TokenParser;
import com.expleague.sensearch.donkey.utils.TokenParser.Token;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Builder;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.SerializedText;
import com.google.common.primitives.Longs;
import gnu.trove.TCollections;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageWriter implements Writer<CrawlerDocument> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageWriter.class);
  private static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);
  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
      .sync(true);
  private static final int BATCH_SIZE = 1000;

  private final BrandNewIdGenerator idGenerator = BrandNewIdGenerator.getInstance();
  private final TokenParser tokenParser;
  private final Writer<Page.Link> linkWriter;
  private final DB pageDb;
  private final Path root;

  private WriteBatch writeBatch;

  public PageWriter(Path outputPath, TokenParser tokenParser, Writer<Page.Link> linkWriter) {
    this.root = outputPath;
    this.linkWriter = linkWriter;
    this.tokenParser = tokenParser;
    try {
      pageDb = JniDBFactory.factory.open(outputPath.toFile(), DB_OPTIONS);
      writeBatch = pageDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void write(CrawlerDocument document) {
    final long pageId = idGenerator.generatePageId(document.uri());
    final List<String> categories = document.categories();
    final List<Page> builtPages = new ArrayList<>();
    final Stack<Builder> parentPagesStack = new Stack<>();
    final List<CharSequence> parentTitles = new ArrayList<>();

    document.sections().forEachOrdered(s -> {
      long sectionId = idGenerator.generatePageId(s.uri());
      Page.Builder section = processSection(s, pageId, sectionId, categories);
      List<CharSequence> sectionTitles = s.titles();

      if (!parentPagesStack.empty()) {
        int commonPrefix = commonPrefix(sectionTitles, parentTitles);
        if (commonPrefix == 0) {
          // We encountered a case when a single page consists of two level-1 titles
          // which is abnormal
          // TODO: how should we handle this situation?
          LOGGER.error(String.format(
              "Received page with id [ %s ] and uri [ %s ] has at least two first-level titles."
                  + " Normally this should never happen. Probably page is corrupted",
              pageId, document.uri()
          ));
        }

        while (parentPagesStack.size() > commonPrefix) {
          builtPages.add(parentPagesStack.pop().build());
        }

        int depthDiff = sectionTitles.size() - parentPagesStack.size();
        if (depthDiff > 1) {
          LOGGER.error(String.format(
              "Received page with id [ %d ] and uri [ %s ] which probably has missing sections."
                  + " Previous section titles: [ %s ]. Current section titles: [ %s ]."
                  + " Depth difference: [ %d ]. Index might be corrupted!",
              pageId, document.uri(), parentTitles.toString(), sectionTitles.toString(),
              depthDiff));
          // TODO: if there were a missing section then we should add it to the stack otherwise parents
          // might become incorrect
        }

        // normally this should never happen!
        if (!parentPagesStack.isEmpty()) {
          section.setParentId(parentPagesStack.peek().getPageId());
          parentPagesStack.peek().addSubpagesIds(sectionId);
        }
      }
      parentPagesStack.push(section);
      // TODO: probably should do it more intelligently
      parentTitles.clear();
      parentTitles.addAll(sectionTitles);
    });

    while (!parentPagesStack.isEmpty()) {
      builtPages.add(Objects.requireNonNull(parentPagesStack.pop()).build());
    }
    builtPages.forEach(
        p -> writeBatch.put(Longs.toByteArray(p.getPageId()), p.toByteArray())
    );
    pageDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = pageDb.createWriteBatch();
  }

  private Page.Builder processSection(Section section, long currentPageId, long sectionId,
      List<String> categories) {

    SerializedText sectionContent = tokenParser.parse(section.text())
        .collect(SerializedTextCollector.instance());
    tokenParser.check(section.text(), toIntArray(sectionContent));

    SerializedText sectionTitle = tokenParser.parse(section.title())
        .collect(SerializedTextCollector.instance());
    tokenParser.check(section.title(), toIntArray(sectionTitle));

    Page.Builder pageBuilder =
        Page.newBuilder()
            .setPageId(sectionId)
            .setContent(sectionContent)
            .setTitle(sectionTitle)
            .setUri(section.uri().toString())
            .addAllCategories(categories);

    section.links().stream()
        .map(l -> new TargetIdLinkPair(idGenerator.generatePageId(l.targetUri()), l))
        .filter(l -> l.targetId() != currentPageId)
        .map(l -> {
          SerializedText sectionLink = tokenParser.parse(l.link().text())
              .collect(SerializedTextCollector.instance());
          tokenParser.check(l.link.text(), toIntArray(sectionLink));

          // TODO: remove offsets maybe?
          return Page.Link.newBuilder()
              .setPosition(l.link().textOffset())
              .setText(sectionLink)
              .setSourcePageId(sectionId)
              .setTargetPageId(l.targetId())
              .build();
        })
        .peek(pageBuilder::addOutgoingLinks)
        .forEach(linkWriter::write);

    return pageBuilder;
  }

  @Override
  public void close() throws IOException {
    pageDb.write(writeBatch, WRITE_OPTIONS);
    linkWriter.close();
    pageDb.close();
    tokenParser.close();
  }

  @Override
  public void flush() throws IOException {
    pageDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = pageDb.createWriteBatch();
    linkWriter.flush();
  }

  private static int commonPrefix(List<CharSequence> list1, List<CharSequence> list2) {
    int minSize = Math.min(list1.size(), list2.size());
    int cmnPref = 0;
    for (; cmnPref < minSize && list1.get(cmnPref).equals(list2.get(cmnPref)); ++cmnPref) {
      ;
    }
    return cmnPref;
  }

  private static int[] toIntArray(SerializedText serializedText) {
    return serializedText.getTokenIdsList().stream().mapToInt(Integer::intValue).toArray();
  }

  private static class TargetIdLinkPair {

    final long targetId;
    final Link link;

    TargetIdLinkPair(long targetId, Link link) {
      this.targetId = targetId;
      this.link = link;
    }

    long targetId() {
      return targetId;
    }

    Link link() {
      return link;
    }
  }

  private static class SerializedTextCollector implements
      Collector<Token, TIntList, SerializedText> {

    private static final SerializedTextCollector INSTANCE = new SerializedTextCollector();
    private static final Set<Characteristics> CHARACTERISTICS = new HashSet<Characteristics>() {{
      add(Characteristics.CONCURRENT);
    }};

    public static SerializedTextCollector instance() {
      return INSTANCE;
    }

    @Override
    public Supplier<TIntList> supplier() {
      return () -> TCollections.synchronizedList(new TIntArrayList());
    }

    @Override
    public BiConsumer<TIntList, Token> accumulator() {
      return (l, t) -> l.add(t.formId());
    }

    @Override
    public BinaryOperator<TIntList> combiner() {
      return (l1, l2) -> {
        l1.addAll(l2);
        return l1;
      };
    }

    @Override
    public Function<TIntList, SerializedText> finisher() {
      return list -> {
        SerializedText.Builder builder = SerializedText.newBuilder();
        //because TIntList is not iterable!
        list.forEach(t -> {
          builder.addTokenIds(t);
          return true;
        });
        return builder.build();
      };
    }

    @Override
    public Set<Characteristics> characteristics() {
      return CHARACTERISTICS;
    }
  }
}
