package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.donkey.utils.TokenParser;
import com.expleague.sensearch.donkey.utils.TokenParser.Token;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.SerializedText;
import com.google.common.primitives.Longs;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageWriter implements Closeable, Flushable {

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
  private final TermWriter termWriter;
  private final LinkWriter linkWriter;
  private final DB pageDb;
  private final Path root;

  private WriteBatch writeBatch;

  public PageWriter(Path root, TokenParser tokenParser, TermWriter termWriter, LinkWriter linkWriter) {
    this.root = root;
    this.termWriter = termWriter;
    this.linkWriter = linkWriter;
    this.tokenParser = tokenParser;
    try {
      pageDb = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
      writeBatch = pageDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void writeDocument(CrawlerDocument document) {
    long pageId = idGenerator.generatePageId(document.uri());
    List<String> categories = document.categories();
    List<Page> builtPages = new ArrayList<>();
    Deque<Page.Builder> parentPagesStack = new LinkedList<>();
    document.sections().forEachOrdered(s -> processSection(
        s, pageId, categories, parentPagesStack, builtPages
    ));
    while (!parentPagesStack.isEmpty()) {
      builtPages.add(Objects.requireNonNull(parentPagesStack.pollLast()).build());
    }
    builtPages.forEach(
        p -> writeBatch.put(Longs.toByteArray(p.getPageId()), p.toByteArray())
    );
    pageDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = pageDb.createWriteBatch();
  }

  private static SerializedText toSerializedText(List<Token> tokens) {
    SerializedText.Builder builder = SerializedText.newBuilder();
    tokens.stream().map(Token::formId).forEach(builder::addTokenIds);
    return builder.build();
  }

  private void processSection(Section section, long currentPageId, List<String> categories,
      Deque<Page.Builder> parentPagesStack, List<Page> builtPages) {
    List<? extends CharSequence> sectionTitleSeq = section.titles();
    int sectionDepth = sectionTitleSeq.size();
    long sectionId = idGenerator.generatePageId(section.uri());
    if (sectionDepth - parentPagesStack.size() > 1) {
      LOGGER.error(
          String.format(
              "Received page (id [ %d ] section id [ %d ] with the depth [ %d ], when current depth is [ %d ]."
                  + " Probably some sections are missing or sections order is incorrect. Index may become corrupted",
              currentPageId, sectionId, sectionDepth, parentPagesStack.size()));
      // TODO: if there were a missing section then we should add it to the stack otherwise parents
      // might become incorrect
    }

    // section depth is always greater than 1
    while (parentPagesStack.size() >= sectionDepth) {
      builtPages.add(Objects.requireNonNull(parentPagesStack.pollLast()).build());
    }
    CharSequence sectionTitle = sectionTitleSeq.get(sectionDepth - 1);
    List<Token> textTokens = tokenParser.parse(section.text());
    List<Token> titleTokens = tokenParser.parse(sectionTitle);
    Page.Builder pageBuilder =
        Page.newBuilder()
            .setPageId(sectionId)
            .setContent(toSerializedText(textTokens))
            .setTitle(toSerializedText(titleTokens))
            .setUri(section.uri().toString())
            .addAllCategories(categories);
    Stream.concat(textTokens.stream(), titleTokens.stream()).forEach(termWriter::writeTerm);

    section.links().stream()
        .filter(l -> idGenerator.generatePageId(l.targetUri()) != currentPageId)
        .map(l -> {
          List<Token> linkTokens = tokenParser.parse(l.text());
          Page.Link builtLink = Page.Link.newBuilder()
              .setPosition(l.textOffset())
              .setText(toSerializedText(linkTokens))
              .setSourcePageId(sectionId)
              .setTargetPageId(idGenerator.generatePageId(l.targetUri()))
              .build();
          return Pair.of(linkTokens, builtLink);
        })
        .peek(p -> pageBuilder.addOutgoingLinks(p.getRight()))
        .peek(p -> linkWriter.writeLink(p.getRight()))
        .flatMap(p -> p.getLeft().stream())
        .forEach(termWriter::writeTerm);

    if (!parentPagesStack.isEmpty()) {
      pageBuilder.setParentId(parentPagesStack.peekLast().getPageId());
      parentPagesStack.peekLast().addSubpagesIds(sectionId);
    }

    parentPagesStack.addLast(pageBuilder);
  }

  @Override
  public void close() throws IOException {
    pageDb.write(writeBatch, WRITE_OPTIONS);
    termWriter.close();
    linkWriter.close();
    pageDb.close();
  }

  @Override
  public void flush() throws IOException {
    pageDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = pageDb.createWriteBatch();
    termWriter.flush();
    linkWriter.flush();
  }
}
