package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainPageBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(PlainPageBuilder.class);

  private static final String TEMP_INDEX_FILE = "TmpIndex";

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final DB plainDb;

  private final Path temporaryIndexRoot;

  private OutputStream temporaryIndexOs;

  // accumulates for all pages
  private List<Page.Link.Builder> linkBuilders = new ArrayList<>();
  private TLongLongMap wikiIdToIndexId = new TLongLongHashMap();

  // accumulates for single page then clears when next page is received
  private List<String> categories = Collections.emptyList();
  private Deque<IndexUnits.Page.Builder> parentPagesStack = new LinkedList<>();
  private List<Page> builtPages = new ArrayList<>();

  /**
   * PlainPageBuilder provides builder for the database of indexed pages.
   * It processes each page section-wise. To start processing new page call
   * {@link #startPage(long, long, List)},
   * To add sections of the page call {@link #addSection(long, Section)}
   * And to finish the page call {@link #endPage()}
   *
   * Call {@link #build()} to create database of received pages
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
   * Receives list of links and splits it into two maps. Outgoing links map has
   * link's source page id as a key, so for each id it stores all of the outgoing links.
   * Incoming links map, on the other hand, stores all incoming links for each id in the
   * similar fashion
   *
   * @param links list of Link.Builder each of which has wiki page id as a target id
   * @param wikiIdToIndexIdMappings mapping from wiki ids to index ids
   * @param outgoingLinks map of outgoing links links
   * @param incomingLinks map of incoming links
   */
  @VisibleForTesting
  static void resolveLinks(List<Page.Link.Builder> links, TLongLongMap wikiIdToIndexIdMappings,
      @NotNull TLongObjectMap<List<Page.Link>> outgoingLinks,
      @NotNull TLongObjectMap<List<Page.Link>> incomingLinks) {
    outgoingLinks.clear();
    incomingLinks.clear();
    for (Page.Link.Builder link : links) {
      long wikiTargetId = link.getTargetPageId();
      if (!wikiIdToIndexIdMappings.containsKey(wikiTargetId)) {
        LOG.warn(
            String.format("Mappings to index id for WikiPage with id [ %d ] was not found!",
                wikiTargetId)
        );
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
   * @param originalId original id of a page. Id is required for building links
   * between pages when build() is called
   * @param indexId mapping from original id to inner index id
   * @param categories the categories page belongs to
   */
  void startPage(long originalId, long indexId, List<? extends CharSequence> categories) {
    wikiIdToIndexId.put(originalId, indexId);
    this.categories = categories.stream().map(CharSequence::toString).collect(Collectors.toList());
    parentPagesStack.clear();
    builtPages.clear();
  }

  /**
   * Adds new section to the started page. The methods provides tying section int tree-like manner
   * that is each page has links to its children and to its parent
   *
   * @param sectionId id of this section that will be used in index
   * @param section section of a crawler document
   */
  void addSection(long sectionId, CrawlerDocument.Section section) {
    for (Link link : section.links()) {
      Page.Link.Builder linkBuilder = Page.Link.newBuilder()
          .setPosition(link.textOffset())
          .setText(link.text().toString())
          .setSourcePageId(sectionId)
          .setTargetPageId(link.targetId());
      linkBuilders.add(linkBuilder);
    }

    List<? extends CharSequence> sectionTitleSeq = section.title();
    int sectionDepth = sectionTitleSeq.size();

    if (sectionDepth - parentPagesStack.size() > 1) {
      LOG.warn(
          String.format(
              "Received page with the depth [ %d ], when current depth is [ %d ]."
                  + " Probably some sections are missing or sections order is incorrect",
              sectionDepth,
              parentPagesStack.size()
          )
      );
    }

    // section depth is always greater than 1
    while (parentPagesStack.size() >= sectionDepth) {
      builtPages.add(parentPagesStack.pollLast().build());
    }

    CharSequence sectionTitle = sectionTitleSeq.get(sectionDepth - 1);
    Page.Builder pageBuilder = Page.newBuilder()
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
  }

  /**
   * Signals the end of the page. Does necessary aggregation of sections.
   * Clears all temporary information about the page
   */
  void endPage() {
    while (!parentPagesStack.isEmpty()) {
      builtPages.add(parentPagesStack.pollLast().build());
    }

    builtPages.forEach(p -> {
          try {
            p.writeDelimitedTo(temporaryIndexOs);
          } catch (IOException e) {
            throw new RuntimeException(
                String.format(
                    "Failed to save page to temporary index [ %s ]. Cause: %s",
                    temporaryIndexRoot.toAbsolutePath().toString(),
                    e.toString()
                )
            );
          }
        }
    );

    try {
      temporaryIndexOs.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to flush indexed pages to temporary directory: [ %s ]",
              temporaryIndexRoot.toAbsolutePath().toString()
          ), e
      );
    }

    builtPages.clear();
    parentPagesStack.clear();
    categories.clear();
  }

  void build() throws IOException {
    temporaryIndexOs.close();

    TLongObjectMap<List<Page.Link>> incomingLinks = new TLongObjectHashMap<>();
    TLongObjectMap<List<Page.Link>> outgoingLinks = new TLongObjectHashMap<>();
    resolveLinks(linkBuilders, wikiIdToIndexId, outgoingLinks, incomingLinks);

    Page.Builder pageBuilder = Page.newBuilder();
    Page rawPage;
    WriteBatch writeBatch = plainDb.createWriteBatch();
    int maxPagesInBatch = 1000;
    int pagesInBatch = 0;
    try (
        InputStream temporaryIndexIs = Files.newInputStream(
            temporaryIndexRoot.resolve(TEMP_INDEX_FILE)
        )
    ) {
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
