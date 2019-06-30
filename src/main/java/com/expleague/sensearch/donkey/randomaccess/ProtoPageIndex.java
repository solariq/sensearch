package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.google.common.primitives.Longs;
import java.nio.file.Path;

public final class ProtoPageIndex extends ProtobufIndex<Long, Page> {

  public ProtoPageIndex(Path root) {
    super(root, Page.class);
  }

  @Override
  protected byte[] encodeKey(Long key) {
    return Longs.toByteArray(key);
  }
}
