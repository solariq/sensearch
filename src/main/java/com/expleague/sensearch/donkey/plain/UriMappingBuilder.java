package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.UriPageMapping;
import com.google.common.primitives.Longs;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
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

  public void startPage(long pageId, URI uri) {
    if (pageUri.containsKey(uri)) {
      throw new IllegalStateException("Uri " + uri + " is already in IndexMeta");
    }
    try {
      String uriDecoded = URLDecoder.decode(uri.toString(), "UTF-8");
      pageUri.put(uriDecoded, pageId);
    } catch (UnsupportedEncodingException e) {
      LOG.warn(e);
    }
    pageUri.put(uri.toString(), pageId);
  }

  public void addSection(URI sectionUri, long sectionId) {
    try {
      String uriDecoded = URLDecoder.decode(sectionUri.toString(), "UTF-8");
      pageUri.put(uriDecoded, sectionId);
    } catch (UnsupportedEncodingException e) {
      LOG.warn(e);
    }
    pageUri.put(sectionUri.toString(), sectionId);
  }

  @Override
  public void close() throws Exception {
    final WriteBatch[] writeBatch = new WriteBatch[] {uriMappingDb.createWriteBatch()};
    int maxPagesInBatch = 1000;
    int[] pagesInBatch = new int[1];

    pageUri.forEachEntry(
        (uri, id) -> {
          writeBatch[0].put(
              Longs.toByteArray(id),
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

    uriMappingDb.close();
  }
}
