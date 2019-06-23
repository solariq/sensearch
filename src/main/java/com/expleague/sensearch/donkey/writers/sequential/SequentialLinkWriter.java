package com.expleague.sensearch.donkey.writers.sequential;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SequentialLinkWriter implements SequentialWriter<Link> {

  private static final String LINKS_FILE = "links.pb";
  private final Path root;
  private final Path outputFile;
  private final OutputStream outputStream;

  public SequentialLinkWriter(Path root) {
    this.root = root;
    this.outputFile = root.resolve(LINKS_FILE);
    try {
      Files.createDirectories(root);
      this.outputStream = new BufferedOutputStream(Files.newOutputStream(outputFile));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void append(Link object) {
    try {
      object.writeTo(outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void flush() {
    try {
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    try {
      outputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
