package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.file.Path;

public class TermStatisticsIndex extends LevelDbBasedIndex<TermStatistics> {
  public TermStatisticsIndex(Path root) {
    super(root);
  }

  @Override
  protected TermStatistics decodeValue(byte[] bytes) {
    try {
      return TermStatistics.parseFrom(bytes);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected byte[] encodeValue(TermStatistics value) {
    return value.toByteArray();
  }
}
