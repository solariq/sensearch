package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.file.Path;

public class TermIndex extends LevelDbBasedIndex<Page.Link> {

  public TermIndex(Path root) {
    super(root);
  }

  @Override
  protected Link decodeValue(byte[] bytes) {
    try {
      return Link.parseFrom(bytes);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected byte[] encodeValue(Page.Link value) {
    return value.toByteArray();
  }
}
