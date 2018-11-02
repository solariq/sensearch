package com.expleague.sensearch.miner;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.miner.impl.RawTextFeaturesMiner;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by sandulmv on 02.11.18.
 */
public class MinerPhase implements SearchPhase {

  private final Index index;
  private final FeaturesMiner featuresExtractor;

  public MinerPhase(Index index) {
    this.index = index;
    this.featuresExtractor = new RawTextFeaturesMiner(index);
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    final Query query = whiteboard.query();
    whiteboard.putTextFeatures(
        index.fetchDocuments(query)
            .map(p -> Pair.of(p, featuresExtractor.extractFeatures(query, p)))
    );
  }
}
