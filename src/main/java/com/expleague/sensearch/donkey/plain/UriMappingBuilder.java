package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.UriPageMapping;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.net.URI;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

public class UriMappingBuilder implements AutoCloseable {
  private static final Logger LOG = Logger.getLogger(UriMappingBuilder.class);

  private final TObjectLongMap<String> pageUri = new TObjectLongHashMap<>();
  private final DB uriMappingDb;

  public UriMappingBuilder(DB uriMappingDb) {
    this.uriMappingDb = uriMappingDb;
  }

  public void addSection(URI sectionUri, long sectionId) {
    pageUri.put(sectionUri.toASCIIString(), sectionId);
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing URI mapping...");
    final WriteBatch[] writeBatch = new WriteBatch[] {uriMappingDb.createWriteBatch()};
    int maxPagesInBatch = 1000;
    int[] pagesInBatch = new int[1];

    pageUri.forEachEntry(
        (uri, id) -> {
//          System.err.println(uri);
          writeBatch[0].put(
              uri.getBytes(),
              UriPageMapping.newBuilder().setUri(uri).setPageId(id).build().toByteArray());
          pagesInBatch[0]++;
          if (pagesInBatch[0] >= maxPagesInBatch) {
            uriMappingDb.write(writeBatch[0]);
            pagesInBatch[0] = 0;
            try {
              writeBatch[0].close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            writeBatch[0] = uriMappingDb.createWriteBatch();
          }
          return true;
        });
    if (pagesInBatch[0] > 0) {
      uriMappingDb.write(writeBatch[0]);
    }
    uriMappingDb.close();
  }
}
