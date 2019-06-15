package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.google.common.primitives.Longs;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageWriter extends LevelDbBasedWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageWriter.class);


  public PageWriter(Path root) {
    super(root);
  }

  public void writeDocument(CrawlerDocument document) {
    long pageId = idGenerator.generatePageId(document.uri());
    List<String> categories = document.categories();
    List<Page> builtPages = new ArrayList<>();
    Deque<Page.Builder> parentPagesStack = new LinkedList<>();
    document.sections().forEachOrdered(s -> addSection(
        s, pageId, categories, parentPagesStack, builtPages, idGenerator
    ));
    while (!parentPagesStack.isEmpty()) {
      builtPages.add(Objects.requireNonNull(parentPagesStack.pollLast()).build());
    }
    builtPages.forEach(
        p -> writeBatch.put(Longs.toByteArray(p.getPageId()), p.toByteArray())
    );
    rootDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = rootDb.createWriteBatch();
  }


  private static void addSection(Section section, // data for processing
      long currentPageId, List<String> categories, // static state
      Deque<Page.Builder> parentPagesStack, List<Page> builtPages, // dynamic state
      BrandNewIdGenerator idGenerator
  ) {
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
    Page.Builder pageBuilder =
        Page.newBuilder()
            .setPageId(sectionId)
            .setContent(section.text().toString())
            .setTitle(sectionTitle.toString())
            .setUri(section.uri().toString())
            .addAllCategories(categories);

    for (Link link : section.links()) {
      long targetId = idGenerator.generatePageId(link.targetUri());
      if (targetId == currentPageId) {
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
    }

    if (!parentPagesStack.isEmpty()) {
      pageBuilder.setParentId(parentPagesStack.peekLast().getPageId());
      parentPagesStack.peekLast().addSubpagesIds(sectionId);
    }

    parentPagesStack.addLast(pageBuilder);
  }

}
