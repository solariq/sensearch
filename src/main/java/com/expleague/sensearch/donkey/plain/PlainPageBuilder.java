package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
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
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainPageBuilder {

  public static final int ROOT_PAGE_ID_OFFSET_BITS = 16;

  private static final Logger LOG = LoggerFactory.getLogger(PlainPageBuilder.class);

  private static final String TEMP_INDEX_FILE = "TmpIndex";

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final DB plainDb;

  private final Path temporaryIndexRoot;

  private List<Page.Link.Builder> linksList = new ArrayList<>();

  private TLongLongMap wikiIdToIndexId = new TLongLongHashMap();

  private OutputStream temporaryIndexOs;

  /**
   * @param plainDb database where index pages will be stored
   * @param tempFilesRoot root where temporary files while building will be stored
   * if directory does not exist it will be created. It will be deleted after build()
   * is called
   * @throws IOException if it is failed to create root directory
   */
  PlainPageBuilder(DB plainDb, Path tempFilesRoot) throws IOException {
    this.plainDb = plainDb;
    this.temporaryIndexRoot = tempFilesRoot;
    Files.createDirectories(tempFilesRoot);
    temporaryIndexOs = Files.newOutputStream(tempFilesRoot.resolve(TEMP_INDEX_FILE));
  }
  
  long add(CrawlerDocument parsedPage) {
    // create new page id
    long newRootPageId = -((wikiIdToIndexId.size() + 1) << ROOT_PAGE_ID_OFFSET_BITS);
    wikiIdToIndexId.put(parsedPage.id(), newRootPageId);

    for (Page page : toPages(parsedPage.sections(), newRootPageId, linksList)) {
      try {
        page.writeDelimitedTo(temporaryIndexOs);
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

    try {
      temporaryIndexOs.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to flush indexed pages to temporary directory: [ %s ]",
              temporaryIndexRoot.toAbsolutePath().toString()
          ), e
      );
    }

    return newRootPageId;
  }

  void build() throws IOException {
    temporaryIndexOs.close();

    TLongObjectMap<List<Page.Link>> incomingLinks = new TLongObjectHashMap<>();
    TLongObjectMap<List<Page.Link>> outcomingLinks = new TLongObjectHashMap<>();
    resolveLinks(linksList, wikiIdToIndexId, outcomingLinks, incomingLinks);

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
        if (outcomingLinks.containsKey(pageId)) {
          pageBuilder.addAllOutcomingLinks(outcomingLinks.get(pageId));
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

  /**
   * Receives list of links and splits it into two maps. Outcoming links map has
   * link's source page id as a key, so for each id it stores all of the outcoming links.
   * Incoming links map, on the other hand, stores all incoming links for each id in the
   * similar fashion
   * @param links list of Link.Builder each of which has wiki page id as a target id
   * @param wikiIdToIndexIdMappings mapping from wiki ids to index ids
   * @param outcomingLinks map of outcoming links
   * @param incomingLinks map of incoming links
   */
  @VisibleForTesting
  static void resolveLinks(List<Page.Link.Builder> links, TLongLongMap wikiIdToIndexIdMappings,
      @NotNull TLongObjectMap<List<Page.Link>> outcomingLinks,
      @NotNull TLongObjectMap<List<Page.Link>> incomingLinks) {
    outcomingLinks.clear();
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
      outcomingLinks.putIfAbsent(sourceId, new ArrayList<>());
      outcomingLinks.get(sourceId).add(builtLink);
    }
  }

  /**
   * Converts given links to list of pages which are connected in tree manner Also enriches given
   * list of know interpage links with links from given sections
   *
   * @param sections list of section from one page
   * @param currentRootPageId id of the first section i.e. section right after page title. All other
   * sctions id will be less than it
   * @param knownLinks all know links
   * @return list of pages
   */
  @VisibleForTesting
  static List<Page> toPages(List<Section> sections, long currentRootPageId,
      List<Page.Link.Builder> knownLinks) {
    LinkedList<Page.Builder> parentPages = new LinkedList<>();
    List<Page> builtPages = new LinkedList<>();

    long currentSectionId = currentRootPageId;
    // TODO: check if sections count is more than set subpages limit
    for (Section section : sections) {
      // enrich known links list
      for (Link link : section.links()) {
        Page.Link.Builder knownLink = Page.Link.newBuilder()
            .setPosition(link.textOffset())
            .setText(link.text().toString())
            .setSourcePageId(currentSectionId)
            .setTargetPageId(link.targetId());
        knownLinks.add(knownLink);
      }

      List<CharSequence> sectionTitleSeq = section.title();
      int sectionDepth = sectionTitleSeq.size();

      if (sectionDepth - parentPages.size() > 1) {
        LOG.warn(
            String.format(
                "Received page with the depth [ %d ], when current depth is [ %d ]."
                    + " Probably some sections are missing or sections order is incorrect",
                sectionDepth,
                parentPages.size()
            )
        );
      }

      // section depth is always greater than 1
      while (parentPages.size() >= sectionDepth) {
        builtPages.add(parentPages.pollLast().build());
      }

      CharSequence sectionTitle = sectionTitleSeq.get(sectionDepth - 1);
      Page.Builder pageBuilder = Page.newBuilder()
          .setPageId(currentSectionId)
          .setContent(section.text().toString())
          .setTitle(sectionTitle.toString());

      if (!parentPages.isEmpty()) {
        pageBuilder.setParentId(parentPages.peekLast().getPageId());
        parentPages.peekLast().addSubpagesIds(currentSectionId);
      }

      parentPages.addLast(pageBuilder);

      // page ids are below zero
      --currentSectionId;
    }

    while (!parentPages.isEmpty()) {
      builtPages.add(parentPages.pollLast().build());
    }

    return builtPages;
  }
}
