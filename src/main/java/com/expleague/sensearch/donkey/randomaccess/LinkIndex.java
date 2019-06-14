package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LinkIndex extends LevelDbBasedIndex<List<Link>> {

  public LinkIndex(Path root) {
    super(root);
  }

  @Override
  protected List<Link> decodeValue(byte[] bytes) {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
    List<Link> links = new ArrayList<>();
    Link link;
    try {
      while ((link =Link.parseDelimitedFrom(byteStream)) != null) {
        links.add(link);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return links;
  }

  @Override
  protected byte[] encodeValue(List<Link> value) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    value.forEach(l -> {
      try {
        l.writeDelimitedTo(byteArrayOutputStream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    return byteArrayOutputStream.toByteArray();
  }
}
