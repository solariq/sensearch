package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make thread safe
public class LinkWriter implements Flushable, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkWriter.class);

  private static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);
  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
      .sync(true);
  private static final int BATCH_SIZE = 1000;
  private static final byte[] EMPTY_VALUE = new byte[0];

  private final Path root;
  private final DB linksDb;
  private final BrandNewIdGenerator idGenerator = BrandNewIdGenerator.getInstance();
  private WriteBatch writeBatch;

  public LinkWriter(Path root) {
    this.root = root;
    try {
      linksDb = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
      writeBatch = linksDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: there should be preprocessed page?
  public void writeLinks(CrawlerDocument document) throws IOException {
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

  @Override
  public void close() throws IOException {
    linksDb.write(writeBatch, WRITE_OPTIONS);
    linksDb.close();
  }

  @Override
  public void flush() throws IOException {
    linksDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = linksDb.createWriteBatch();
  }
}
