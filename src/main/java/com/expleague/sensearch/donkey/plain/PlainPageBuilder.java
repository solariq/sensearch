package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Builder;
import com.google.common.primitives.Longs;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainPageBuilder implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(PlainPageBuilder.class);

  private static final String TEMP_INDEX_FILE = "TmpIndex";

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final DB plainDb;
  private final Path temporaryIndexRoot;
  private OutputStream temporaryIndexOs;

  private final List<Page> builtPages = Collections.synchronizedList(new ArrayList<>());
  private final Map<Long, List<Page.Link>> incomingLinksForPage = new ConcurrentHashMap<>();

  // accumulates for single page then clears when next page is received
  private ThreadLocal<List<String>> categories = new ThreadLocal<>();
  private final ThreadLocal<Deque<Builder>> parentPagesStack = ThreadLocal
      .withInitial(LinkedList::new);
  private ThreadLocal<Boolean> isProcessingPage = ThreadLocal.withInitial(() -> false);
  private ThreadLocal<Long> curPageId = new ThreadLocal<>();

  /**
   * PlainPageBuilder provides builder for the database of indexed pages. It processes each page
   * section-wise. To start processing new page call {@link #startPage}, To add sections of the page
   * call {@link #addSection} And to finish the page call {@link #endPage()}
   *
   * @param plainDb database where index pages will be stored
   * @param tempFilesRoot root where temporary files while building will be stored if directory does
   * not exist it will be created. It will be deleted after build() is called
   * @throws IOException if it is failed to create root directory
   */
  PlainPageBuilder(DB plainDb, Path tempFilesRoot) throws IOException {
    this.plainDb = plainDb;
    this.temporaryIndexRoot = tempFilesRoot;

    Files.createDirectories(tempFilesRoot);
    temporaryIndexOs = Files.newOutputStream(tempFilesRoot.resolve(TEMP_INDEX_FILE));
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
//            .setContent(section.text().toString())
//            .setTitle(sectionTitle.toString())
            .setUri(section.uri().toString())
            .addAllCategories(categories.get());

    BrandNewIdGenerator idGenerator = BrandNewIdGenerator.getInstance();
    for (Link link : section.links()) {
      long targetId = idGenerator.generatePageId(link.targetUri());
      if (targetId == curPageId.get()) {
        // Ignoring self-links
        continue;
      }
      Page.Link dbLink =
          Page.Link.newBuilder()
              .setPosition(link.textOffset())
//              .setText(link.text().toString())
              .setSourcePageId(sectionId)
              .setTargetPageId(targetId)
              .build();
      pageBuilder.addOutgoingLinks(dbLink);
      if (!incomingLinksForPage.containsKey(targetId)) {
        incomingLinksForPage.put(targetId, new ArrayList<>());
      }
      incomingLinksForPage.get(targetId).add(dbLink);
    }

    if (!parentPagesStackLocal.isEmpty()) {
      pageBuilder.setParentId(parentPagesStackLocal.peekLast().getPageId());
      parentPagesStackLocal.peekLast().addSubpagesIds(sectionId);
    }

    parentPagesStackLocal.addLast(pageBuilder);
  }

  //Mb it's still appendable

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

    if (builtPages.size() >= PlainIndexCreator.BATCH_SIZE) {
      writeBatch();
    }

    categories.get().clear();
    parentPagesStackLocal.clear();
  }

  private void writeBatch() {
    LOG.info("Save batch:: " + builtPages.size() + " to tmpIndex");
    builtPages.forEach(
        p -> {
          try {
            p.writeDelimitedTo(temporaryIndexOs);
          } catch (IOException e) {
            throw new RuntimeException(
                String.format(
                    "Failed to save page to temporary index [ %s ]. Cause: %s",
                    temporaryIndexRoot.toAbsolutePath().toString(), e.toString()));
          }
        });

    try {
      temporaryIndexOs.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          String.format(
              "Failed to flush indexed pages to temporary directory: [ %s ]",
              temporaryIndexRoot.toAbsolutePath().toString()),
          e);
    }

    builtPages.clear();
  }

  @Override
  public void close() throws IOException {
    writeBatch();
    LOG.info("Storing page links...");
    temporaryIndexOs.close();

    Page.Builder pageBuilder = Page.newBuilder();
    Page rawPage;
    WriteBatch writeBatch = plainDb.createWriteBatch();
    int maxPagesInBatch = 1000;
    int pagesInBatch = 0;
    try (InputStream temporaryIndexIs =
        Files.newInputStream(temporaryIndexRoot.resolve(TEMP_INDEX_FILE))) {
      while ((rawPage = Page.parseDelimitedFrom(temporaryIndexIs)) != null) {
        pageBuilder.clear();
        pageBuilder.mergeFrom(rawPage);
        long pageId = pageBuilder.getPageId();

        if (incomingLinksForPage.containsKey(pageId)) {
          pageBuilder.addAllIncomingLinks(incomingLinksForPage.get(pageId));
        }

        if (pagesInBatch >= maxPagesInBatch) {
          plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
          pagesInBatch = 0;
          writeBatch.close();
          writeBatch = plainDb.createWriteBatch();
        }

        writeBatch.put(Longs.toByteArray(pageId), pageBuilder.build().toByteArray());
        ++pagesInBatch;
      }

      if (pagesInBatch > 0) {
        plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
      }
    } finally {
      FileUtils.deleteDirectory(temporaryIndexRoot.toFile());
      plainDb.close();
    }
  }
}
