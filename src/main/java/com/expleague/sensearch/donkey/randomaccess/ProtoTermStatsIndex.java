package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

public class ProtoTermStatsIndex extends ProtobufIndex<Integer, TermStatistics> {

  private final TIntObjectMap<TermStatistics> idToStats = new TIntObjectHashMap<>();

  public ProtoTermStatsIndex(Path root) {
    super(root, TermStatistics.class);
    forEach(stats -> idToStats.put(stats.getTermId(), stats));
  }

  @Override
  protected byte[] encodeKey(Integer key) {
    return Ints.toByteArray(key);
  }

  @Nullable
  @Override
  public TermStatistics value(Integer id) {
    return idToStats.get(id);
  }

  @Override
  public void put(Integer id, TermStatistics value) {
    idToStats.put(id, value);
    super.put(id, value);
  }

}
