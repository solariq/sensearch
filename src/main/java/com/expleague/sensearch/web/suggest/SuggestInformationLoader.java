package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.term.TermBase;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class SuggestInformationLoader {

  private static final Logger LOG = Logger.getLogger(SuggestInformationLoader.class);

  public final Collection<MultigramWrapper> multigramFreqNorm = new ArrayList<>();

  private final DB multigramFreqNormDB;

  private final TermBase termBase;

  public SuggestInformationLoader(
      DB unigramCoeffDB, DB multigramDB, TermBase termBase) {
    this.multigramFreqNormDB = multigramDB;

    this.termBase = termBase;

    tryLoad();
  }

  private void tryLoad() {
    LOG.info("Loading suggest...");
    long startTime = System.nanoTime();

    {
      DBIterator iter = multigramFreqNormDB.iterator();
      iter.seekToFirst();
      iter.forEachRemaining(
          item -> {
            Term[] terms = null;
            try {
              terms =
                  IndexUnits.TermList.parseFrom(item.getKey())
                      .getTermListList()
                      .stream()
                      .map(termBase::term)
                      .toArray(Term[]::new);
            } catch (InvalidProtocolBufferException e) {
              throw new RuntimeException(e);
            }

            multigramFreqNorm.add(
                new MultigramWrapper(terms, Double.longBitsToDouble(Longs.fromByteArray(item.getValue()))));
          });
    }

    LOG.info("Suggest loading finished in " + (System.nanoTime() - startTime) / 1e9 + " seconds");
  }
}
