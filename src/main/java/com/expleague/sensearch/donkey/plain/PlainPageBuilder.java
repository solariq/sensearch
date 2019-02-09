package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongLongHashMap;
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
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainPageBuilder implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(PlainPageBuilder.class);

  private static final String TEMP_INDEX_FILE = "TmpIndex";

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final DB plainDb;

  private final Path temporaryIndexRoot;
  private final IdGenerator idGenerator;

  private OutputStream temporaryIndexOs;

  // accumulates for all pages
  private final List<Page.Link.Builder> linkBuilders = new ArrayList<>();
  private final TLongLongMap wikiIdToIndexId = new TLongLongHashMap();

  // accumulates for single page then clears when next page is received
  private List<String> categories = Collections.emptyList();
  private final Deque<IndexUnits.Page.Builder> parentPagesStack = new LinkedList<>();
  private final List<Page> builtPages = new ArrayList<>();

  private boolean isProcessingPage = false;
  // Flag that indicates if there as at least one section added for current page
  private boolean hasSection = false;
  private long curPageId;

  /**
   * PlainPageBuilder provides builder for the database of indexed pages. It processes each page
   * section-wise. To start processing new page call {@link #startPage}, To add sections of the page
   * call {@link #addSection} And to finish the page call {@link #endPage()}
   *
   * @param plainDb database where index pages will be stored
   * @param tempFilesRoot root where temporary files while building will be stored if directory does
   *     not exist it will be created. It will be deleted after build() is called
   * @throws IOException if it is failed to create root directory
   */
  PlainPageBuilder(DB plainDb, Path tempFilesRoot, IdGenerator idGenerator) throws IOException {
    this.plainDb = plainDb;
    this.temporaryIndexRoot = tempFilesRoot;
    this.idGenerator = idGenerator;

    Files.createDirectories(tempFilesRoot);
    temporaryIndexOs = Files.newOutputStream(tempFilesRoot.resolve(TEMP_INDEX_FILE));
  }

  /**
   * Receives list of links and splits it into two maps. Outgoing links map has link's source page
   * id as a key, so for each id it stores all of the outgoing links. Incoming links map, on the
   * other hand, stores all incoming links for each id in the similar fashion
   *
   * @param links list of Link.Builder each of which has wiki page id as a target id
   * @param wikiIdToIndexIdMappings mapping from wiki ids to index ids
   * @param outgoingLinks map of outgoing links links
   * @param incomingLinks map of incoming links
   */
  @VisibleForTesting
  static void resolveLinks(
      List<Page.Link.Builder> links,
      TLongLongMap wikiIdToIndexIdMappings,
      @NotNull TLongObjectMap<List<Page.Link>> outgoingLinks,
      @NotNull TLongObjectMap<List<Page.Link>> incomingLinks) {
    outgoingLinks.clear();
    incomingLinks.clear();
    for (Page.Link.Builder link : links) {
      long wikiTargetId = link.getTargetPageId();
      if (!wikiIdToIndexIdMappings.containsKey(wikiTargetId)) {
//        LOG.warn(
//            String.format(
//                "Mappings to index id for WikiPage with id [ %d ] was not found!", wikiTargetId));
        link.clearTargetPageId();
      } else {
        long targetIndexId = wikiIdToIndexIdMappings.get(wikiTargetId);
        link.setTargetPageId(targetIndexId);
      }

      Page.Link builtLink = link.build();

      if (builtLink.hasTargetPageId()) {
        long targetId = builtLink.getTargetPageId();
        incomingLinks.putIfAbsent(targetId, new LinkedList<>());
        incomingLinks.get(targetId).add(builtLink);
      }

      long sourceId = link.getSourcePageId();
      outgoingLinks.putIfAbsent(sourceId, new ArrayList<>());
      outgoingLinks.get(sourceId).add(builtLink);
    }
  }

  /**
   * Prepares for receiving sections of the page. Erases all information about the previous page
   *
   * @param originalPageId original id of a page. Id is required for building links between pages
   *     when build() is called
   * @param categories the categories page belongs to
   * @param uri URI of the page. URIs must be unique among the collection
   * @return id associated with this page. This id will be equal to the id of the root (first)
   *     section
   */
  long startPage(long originalPageId, List<? extends CharSequence> categories, URI uri) {
    if (isProcessingPage) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }

    isProcessingPage = true;
    hasSection = false;

    curPageId = idGenerator.pageId(uri);
    wikiIdToIndexId.put(originalPageId, curPageId);
    this.categories = categories.stream().map(CharSequence::toString).collect(Collectors.toList());
    parentPagesStack.clear();
    return curPageId;
  }

  /**
   * Adds new section to the started page. The methods provides tying section int tree-like manner
   * that is each page has links to its children and to its parent
   *
   * @param section section of a crawler document
   * @return id of the section. Id of the root (first) section of the page equals to the id returned
   *     by {@link #startPage}
   */
  long addSection(CrawlerDocument.Section section) {
    long sectionId = hasSection ? idGenerator.sectionId(section.uri()) : curPageId;
    hasSection = true;

    for (Link link : section.links()) {
      Page.Link.Builder linkBuilder =
          Page.Link.newBuilder()
              .setPosition(link.textOffset())
              .setText(link.text().toString())
              .setSourcePageId(sectionId)
              .setTargetPageId(link.targetId());
      linkBuilders.add(linkBuilder);
    }

    List<? extends CharSequence> sectionTitleSeq = section.title();
    int sectionDepth = sectionTitleSeq.size();

    if (sectionDepth - parentPagesStack.size() > 1) {
      LOG.error(
          String.format(
              "Received page (id [ %d ] section id [ %d ] with the depth [ %d ], when current depth is [ %d ]."
                  + " Probably some sections are missing or sections order is incorrect. Index may become corrupted",
              curPageId, sectionId, sectionDepth, parentPagesStack.size()));
      // TODO: if there were a missing section then we should add it to the stack otherwise parents
      // might become incorrect
    }

    // section depth is always greater than 1
    while (parentPagesStack.size() >= sectionDepth) {
      builtPages.add(Objects.requireNonNull(parentPagesStack.pollLast()).build());
    }

    CharSequence sectionTitle = sectionTitleSeq.get(sectionDepth - 1);
    Page.Builder pageBuilder =
        Page.newBuilder()
            .setPageId(sectionId)
            .setContent(section.text().toString())
            .setTitle(sectionTitle.toString())
            .setUri(section.uri().toString())
            .addAllCategories(categories);

    if (!parentPagesStack.isEmpty()) {
      pageBuilder.setParentId(parentPagesStack.peekLast().getPageId());
      parentPagesStack.peekLast().addSubpagesIds(sectionId);
    }

    parentPagesStack.addLast(pageBuilder);

    return sectionId;
  }

  /**
   * Signals the end of the page. Does necessary aggregation of sections. Clears all temporary
   * information about the page
   */
  void endPage() {
    if (!isProcessingPage) {
      throw new IllegalStateException("Illegal call to endPage: no page is being processed");
    }
    isProcessingPage = false;

    while (!parentPagesStack.isEmpty()) {
      builtPages.add(Objects.requireNonNull(parentPagesStack.pollLast()).build());
    }

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
    parentPagesStack.clear();
    categories.clear();
  }

  @Override
  public void close() throws IOException {
    temporaryIndexOs.close();

    TLongObjectMap<List<Page.Link>> incomingLinks = new TLongObjectHashMap<>();
    TLongObjectMap<List<Page.Link>> outgoingLinks = new TLongObjectHashMap<>();
    resolveLinks(linkBuilders, wikiIdToIndexId, outgoingLinks, incomingLinks);

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
        if (outgoingLinks.containsKey(pageId)) {
          pageBuilder.addAllOutgoingLinks(outgoingLinks.get(pageId));
        }

        if (incomingLinks.containsKey(pageId)) {
          pageBuilder.addAllIncomingLinks(incomingLinks.get(pageId));
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
