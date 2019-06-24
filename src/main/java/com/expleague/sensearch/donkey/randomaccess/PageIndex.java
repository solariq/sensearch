package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.file.Path;

public class PageIndex extends LevelDbBasedIndex<Page>{
  public PageIndex(Path path) {
    super(path);
  }

  @Override
  protected Page decodeValue(byte[] bytes) {
    try {
      return Page.parseFrom(bytes);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected byte[] encodeValue(Page value) {
    return value.toByteArray();
  }
}
