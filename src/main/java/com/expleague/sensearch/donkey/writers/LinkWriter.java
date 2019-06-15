package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make thread safe
public class LinkWriter extends LevelDbBasedWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkWriter.class);
  private static final byte[] EMPTY_VALUE = new byte[0];


  public LinkWriter(Path root) {
    super(root);
  }

  // TODO: there should be preprocessed page?
  // TODO: make bigger batch
  public void writeLinks(CrawlerDocument document) {
    final long pageId = idGenerator.generatePageId(document.uri());
    final Link.Builder linkBuilder = Link.newBuilder();
    document.sections()
        .map(Section::links)
        .flatMap(List::stream)
        .forEach(link -> {
          linkBuilder.clear();
          linkBuilder.setSourcePageId(pageId);
          linkBuilder.setTargetPageId(idGenerator.generatePageId(link.targetUri()));
          linkBuilder.setPosition(link.textOffset());
          linkBuilder.setText(link.text().toString());
          writeBatch.put(linkBuilder.build().toByteArray(), EMPTY_VALUE);
        });
    flush();
  }

}
