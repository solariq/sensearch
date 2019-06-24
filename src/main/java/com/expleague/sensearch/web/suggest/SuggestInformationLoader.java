package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TIntObjectMap;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class SuggestInformationLoader {

  private static final Logger LOG = Logger.getLogger(SuggestInformationLoader.class);

  public final Collection<MultigramWrapper> multigramFreqNorm = new ArrayList<>();

  private final DB multigramFreqNormDB;

  private final TIntObjectMap<Term> idToTerm;

  public SuggestInformationLoader(
      DB unigramCoeffDB, DB multigramDB, TIntObjectMap<Term> idToTerm) {
    this.multigramFreqNormDB = multigramDB;

    this.idToTerm = idToTerm;

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
                      .map(idToTerm::get)
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
