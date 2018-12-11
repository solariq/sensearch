package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainPageBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(PlainPageBuilder.class);

  private static final int MAX_PAGES_IN_BATCH = 100;

  private static final int PAGE_ID_OFFSET_BITS = 16;

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final DB plainDb;

  private int flushedCount = 0;

  private WriteBatch writeBatch = null;
  private int pagesInBatch = 0;

  PlainPageBuilder(DB plainDb) {
    this.plainDb = plainDb;
  }

  // TODO: resolve links between pages!
  long add(CrawlerDocument newPage) {
    if (writeBatch == null) {
      writeBatch = plainDb.createWriteBatch();
    }

    if (pagesInBatch >= MAX_PAGES_IN_BATCH) {
      plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
      pagesInBatch = 0;
      writeBatch = plainDb.createWriteBatch();
    }

    ++flushedCount;

    long pageId = flushedCount << PAGE_ID_OFFSET_BITS;

    byte[] pageIdBytes = Longs.toByteArray(flushedCount);
    byte[] pageBytes =
        IndexUnits.Page.newBuilder()
            .setPageId(flushedCount)
            .setContent(newPage.content().toString())
            .setTitle(newPage.title())
            .setUri(newPage.uri().toString())
            .build()
            .toByteArray();

    writeBatch.put(pageIdBytes, pageBytes);
    ++pagesInBatch;

    return flushedCount--;
  }

  void build() throws IOException {
    if (pagesInBatch > 0) {
      plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
    }

    plainDb.close();
  }

  // TODO: Make more sensible names for variables
  private List<Page> toPages(long currentPageId, List<Section> sections) {
    LinkedList<Page.Builder> pagesStack = new LinkedList<>();
    List<Page> builtPages = new LinkedList<>();
    for (Section section : sections) {
      List<CharSequence> sectionTitleSeq = section.title();
      int sectionDepth = sectionTitleSeq.size();

      if (sectionDepth - pagesStack.size() > 1) {
        LOG.warn(
            String.format(
                "Received page with the depth [ %d ], when current depth is [ %d ]."
                    + " Probably some sections are missing or pages order is incorrect",
                sectionDepth,
                pagesStack.size()
            )
        );
      }

      while (pagesStack.size() >= sectionDepth) {
        builtPages.add(pagesStack.pollLast().build());
      }
      // TODO: section uri
      // TODO: Links
      CharSequence sectionTitle = sectionTitleSeq.get(sectionDepth - 1);
      Page.Builder pageBuilder = Page.newBuilder()
          .setPageId(currentPageId)
          .setContent(section.text().toString())
          .setTitle(sectionTitle.toString());

      if (!pagesStack.isEmpty()) {
        pageBuilder.setParentId(pagesStack.peekLast().getPageId());
        pagesStack.peekLast().addSubpagesIds(currentPageId);
      }

      pagesStack.addLast(pageBuilder);

      ++currentPageId;
    }

    while (!pagesStack.isEmpty()) {
      builtPages.add(pagesStack.pollLast().build());
    }

    return builtPages;
  }
}
