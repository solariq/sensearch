package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.IncrementalBuilder;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Builder;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import gnu.trove.TCollections;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainPageBuilder implements AutoCloseable, IncrementalBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(PlainPageBuilder.class);
  private static final long DEFAULT_CACHE_SIZE = 1 << 10; // 1 KB
  private static final int PLAIN_PAGE_BLOCK_SIZE = 1 << 20;
  private static final Options PAGE_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockSize(PLAIN_PAGE_BLOCK_SIZE)
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final String TEMP_INDEX_FILE = "TmpIndex";

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final Path plainPageBaseRoot;
  private final List<PlainPageBuilderState> priorStates = new ArrayList<>();

  // global state
  private final List<Page> builtPages = Collections.synchronizedList(new ArrayList<>());
  private final TLongObjectMap<List<Page.Link>> incomingLinks = TCollections.synchronizedMap(
      new TLongObjectHashMap<>());

  // page-local state
  private final ThreadLocal<List<String>> categories = ThreadLocal
      .withInitial(ArrayList::new);
  private final ThreadLocal<Deque<Builder>> parentPagesStack = ThreadLocal
      .withInitial(LinkedList::new);
  private final ThreadLocal<Boolean> isProcessingPage = ThreadLocal
      .withInitial(() -> false);
  private final ThreadLocal<Long> curPageId = new ThreadLocal<>();

  /**
   * PlainPageBuilder provides builder for the database of indexed pages. It processes each page
   * section-wise. To start processing new page call {@link #startPage}, To add sections of the page
   * call {@link #addSection} And to finish the page call {@link #endPage()}
   */
  PlainPageBuilder(Path plainPageBaseRoot) throws IOException {
    this.plainPageBaseRoot = plainPageBaseRoot;
  }

  /**
   * Prepares for receiving sections of the page. Erases all information about the previous page
   *
   * @param categories the categories page belongs to
   * @param uri URI of the page. URIs must be unique among the collection
   */
  void startPage(long pageId, List<? extends CharSequence> categories, URI uri) {
    if (isProcessingPage.get()) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }
    isProcessingPage.set(true);

    curPageId.set(pageId);
    this.categories
        .set(categories.stream().map(CharSequence::toString).collect(Collectors.toList()));
    parentPagesStack.get().clear();
  }

  /**
   * Adds new section to the started page. The methods provides tying section int tree-like manner
   * that is each page has links to its children and to its parent
   *
   * @param section section of a crawler document
   */
  void addSection(CrawlerDocument.Section section, long sectionId) {

    List<? extends CharSequence> sectionTitleSeq = section.titles();
    int sectionDepth = sectionTitleSeq.size();
    Deque<Builder> parentPagesStackLocal = parentPagesStack.get();
    if (sectionDepth - parentPagesStackLocal.size() > 1) {
      LOG.error(
          String.format(
              "Received page (id [ %d ] section id [ %d ] with the depth [ %d ],"
                  + " when current depth is [ %d ]."
                  + " Probably some sections are missing or sections order is incorrect."
                  + " Index may become corrupted",
              curPageId, sectionId, sectionDepth, parentPagesStackLocal.size()));
      // TODO: if there were a missing section then we should add it to the stack otherwise parents
      // might become incorrect
    }

    // section depth is always greater than 1
    while (parentPagesStackLocal.size() >= sectionDepth) {
      builtPages.add(Objects.requireNonNull(parentPagesStackLocal.pollLast()).build());
    }

    CharSequence sectionTitle = sectionTitleSeq.get(sectionDepth - 1);
    Page.Builder pageBuilder =
        Page.newBuilder()
            .setPageId(sectionId)
            .setContent(section.text().toString())
            .setTitle(sectionTitle.toString())
            .setUri(section.uri().toString())
            .addAllCategories(categories.get());

    for (Link link : section.links()) {
      long targetId = BrandNewIdGenerator.generatePageId(link.targetUri());
      if (targetId == curPageId.get()) {
        // Ignoring self-links
        continue;
      }
      Page.Link dbLink =
          Page.Link.newBuilder()
              .setPosition(link.textOffset())
              .setText(link.text().toString())
              .setSourcePageId(sectionId)
              .setTargetPageId(targetId)
              .build();
      pageBuilder.addOutgoingLinks(dbLink);
      incomingLinks.putIfAbsent(targetId, new ArrayList<>());
      incomingLinks.get(targetId).add(dbLink);
    }

    if (!parentPagesStackLocal.isEmpty()) {
      pageBuilder.setParentId(parentPagesStackLocal.peekLast().getPageId());
      parentPagesStackLocal.peekLast().addSubpagesIds(sectionId);
    }

    parentPagesStackLocal.addLast(pageBuilder);
  }

  /**
   * Signals the end of the page. Does necessary aggregation of sections. Clears all temporary
   * information about the page
   */
  void endPage() {
    if (!isProcessingPage.get()) {
      throw new IllegalStateException("Illegal call to endPage: no page is being processed");
    }
    isProcessingPage.set(false);

    Deque<Builder> parentPagesStackLocal = parentPagesStack.get();
    while (!parentPagesStackLocal.isEmpty()) {
      builtPages.add(Objects.requireNonNull(parentPagesStackLocal.pollLast()).build());
    }

    builtPages.clear();
    categories.get().clear();
  }

  private static void addLinksTo(TLongObjectMap<List<Page.Link>> to,
      TLongObjectMap<List<Page.Link>> increment) {
    increment.forEachEntry(
        (k, l) -> {
          if (to.containsKey(k)) {
            to.get(k).addAll(l);
          } else {
            to.put(k, l);
          }
          return true;
        }
    );
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing page links...");
    priorStates.forEach(s -> addLinksTo(incomingLinks, s.incomingLinks()));

    Page.Builder pageBuilder = Page.newBuilder();
    DB plainDb = JniDBFactory.factory.open(plainPageBaseRoot.toFile(), PAGE_DB_OPTIONS);
    WriteBatch writeBatch = plainDb.createWriteBatch();
    final int maxPagesInBatch = 1000;
    int pagesInBatch = 0;
    for (PlainPageBuilderState state : priorStates) {
      for (Page page : state.builtPages()) {
        pageBuilder.clear();
        pageBuilder.mergeFrom(page);
        if (incomingLinks.containsKey(page.getPageId())) {
          pageBuilder.addAllIncomingLinks(incomingLinks.get(page.getPageId()));
        }
        if (pagesInBatch >= maxPagesInBatch) {
          plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
          pagesInBatch = 0;
          writeBatch.close();
          writeBatch = plainDb.createWriteBatch();
        }

        writeBatch.put(Longs.toByteArray(page.getPageId()), pageBuilder.build().toByteArray());
        ++pagesInBatch;
      }
      state.free();
    }
    if (pagesInBatch > 0) {
      plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
    }
    plainDb.close();
  }

  @Override
  public void setStates(BuilderState... increments) {
    resetState();
    priorStates.clear();
    priorStates.addAll(IncrementalBuilder.accumulate(PlainPageBuilderState.class, increments));
  }

  @Override
  public synchronized BuilderState state() {
    PlainPageBuilderState state = new PlainPageBuilderState(this);
    priorStates.add(state);
    resetState();
    return state;
  }

  private synchronized void resetState() {
    this.builtPages.clear();
    this.incomingLinks.clear();
  }

  static final class PlainPageBuilderState implements BuilderState {
    private static final String INCOMING_LINKS_PROP = "incLnk";
    private static final String PAGES_PROP = "pages";

    private List<Page> builtPages = null;
    private TLongObjectMap<List<Page.Link>> incomingLinks = null;

    private StateMeta meta = null;
    private Path root = null;

    private PlainPageBuilderState(PlainPageBuilder owner) {
      this.builtPages = new ArrayList<>();
      this.builtPages.addAll(owner.builtPages);

      this.incomingLinks = new TLongObjectHashMap<>();
      owner.incomingLinks.forEachEntry(
          (id, l) -> {
            incomingLinks.putIfAbsent(id, new ArrayList<>(l));
            return true;
          }
      );
    }

    private void free() {
      if (root == null || meta == null) {
        throw new IllegalStateException("State is not saved and cannot be freed");
      }

      builtPages = null;
      incomingLinks = null;
    }

    private List<Page> builtPages() {
      if (builtPages != null) {
        return builtPages;
      }

      if (meta == null || root == null) {
        throw new IllegalStateException("Either terms list or meta file must be non null!");
      }

      builtPages = BuilderState.loadProtobufList(meta, PAGES_PROP, root, Page.class);
      return builtPages;
    }

    private TLongObjectMap<List<Page.Link>> incomingLinks() {
      if (incomingLinks != null) {
        return incomingLinks;
      }

      if (meta == null || root == null) {
        throw new IllegalStateException("Either terms list or meta file must be non null!");
      }

      incomingLinks = toIncomingLinksMap(BuilderState.loadProtobufList(meta, INCOMING_LINKS_PROP,
          root, Page.Link.class));
      return incomingLinks;
    }

    private static TLongObjectMap<List<Page.Link>> toIncomingLinksMap(List<Page.Link> links) {
      TLongObjectMap<List<Page.Link>> linksMap = new TLongObjectHashMap<>();
      for (Page.Link link : links) {
        long targetId = link.getTargetPageId();
        linksMap.putIfAbsent(targetId, new ArrayList<>());
        linksMap.get(targetId).add(link);
      }
      return linksMap;
    }

    private PlainPageBuilderState(Path root, StateMeta meta) {
      this.root = root;
      this.meta = meta;
    }

    public static BuilderState loadFrom(Path from) throws IOException {
      return BuilderState.loadFrom(from, PlainPageBuilderState.class, LOG);
    }

    @Override
    public void saveTo(Path to) throws IOException {
      if (Files.exists(to)) {
        throw new IOException(String.format("Path [ %s ] already exists!", to.toString()));
      }

      Files.createDirectories(to);
      String incomingLinksFile = "incomingLinks.pb";
      String pagesFile = "pages.pb";

      meta = StateMeta.builder(PlainPageBuilderState.class)
          .addProperty(INCOMING_LINKS_PROP, incomingLinksFile)
          .addProperty(PAGES_PROP, pagesFile)
          .build();
      meta.writeTo(to.resolve(META_FILE));

      try (OutputStream os = Files.newOutputStream(to.resolve(pagesFile))) {
        for (Page p : builtPages) {
          p.writeDelimitedTo(os);
        }
      }

      try (OutputStream os = Files.newOutputStream(to.resolve(incomingLinksFile))) {
        for (List<Page.Link> linkList : incomingLinks.valueCollection()) {
          for (Page.Link link : linkList) {
            link.writeTo(os);
          }
        }
      }

      root = to;
      free();
    }
  }
}
