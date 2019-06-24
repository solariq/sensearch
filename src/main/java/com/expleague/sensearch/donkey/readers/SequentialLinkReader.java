package com.expleague.sensearch.donkey.readers;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SequentialLinkReader implements Reader<Link> {

  private final Path inputPath;
  private final InputStream inputStream;

  public SequentialLinkReader(Path inputPath) {
    this.inputPath = inputPath;
    try {
      this.inputStream = new BufferedInputStream(Files.newInputStream(inputPath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Link read() {
    Link link;
    try {
      link = Link.parseDelimitedFrom(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return link;
  }

  @Override
  public void close() {
    try {
      inputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
