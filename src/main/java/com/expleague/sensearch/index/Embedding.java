package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import gnu.trove.list.TLongList;
import java.util.stream.LongStream;

public interface Embedding {

  Vec getVec(long... ids);

  Vec getVec(TLongList ids);

  LongStream getNearest(Vec mainVec, int numberOfNeighbors);
}
