package com.expleague.sensearch.donkey.utils;

import com.expleague.sensearch.donkey.readers.Reader;
import com.expleague.sensearch.donkey.writers.sequential.SequentialWriter;
import com.expleague.sensearch.utils.SensearchTestCase;
import gnu.trove.TCollections;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import org.junit.Assert;
import org.junit.Test;

public class ExternalSorterTest extends SensearchTestCase {

  private static class LongReader implements Reader<Long> {

    private final Path inputFile;
    private final Scanner scanner;
    public LongReader(Path inputFile) {
      this.inputFile = inputFile;
      try {
        this.scanner = new Scanner(inputFile.toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Long read() {
      try {
        return scanner.nextLong();
      } catch (NoSuchElementException e) {
        return null;
      }
    }

    @Override
    public void close() throws IOException {
      scanner.close();
    }
  }

  private static class LongWriter implements SequentialWriter<Long> {
    private final BufferedWriter writer;
    public LongWriter(Path outputFile) {
      try {
        writer = Files.newBufferedWriter(outputFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void append(Long object) {
      try {
        writer.append(Long.toString(object));
        writer.append(" ");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() throws IOException {
      writer.close();
    }

    @Override
    public void flush() throws IOException {
      writer.flush();
    }
  }

  @Test
  public void test() throws Exception {
    TLongList list = new TLongArrayList();
    Random r = new Random();
    for (int i = 0; i < 1_000; ++i) {
      list.add(r.nextLong());
    }

    Path root = testOutputRoot();
    Path sourceFile = root.resolve("source");
    try (SequentialWriter<Long> longSequentialWriter = new LongWriter(sourceFile)) {
      list.forEach(i -> {
        longSequentialWriter.append(i);
        return true;
      });
    }

    Path outputFile = root.resolve("output");
    ExternalSorter.sort(
        new LongReader(sourceFile),
        outputFile,
        100,
        Comparator.naturalOrder(),
        LongWriter::new,
        LongReader::new
    );

    list.sort();
    TLongList sortedList = new TLongArrayList(list.size());
    try (Reader<Long> longReader = new LongReader(outputFile)) {
      Long l;
      while ((l = longReader.read()) != null) {
        sortedList.add(l);
      }
    }

    Assert.assertEquals(list, sortedList);
  }
}
