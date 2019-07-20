package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.donkey.readers.Reader;
import com.expleague.sensearch.donkey.writers.sequential.SequentialWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;

// TODO: make thread safe
public class ExternalSorter {

  private static final int DEFAULT_READ_BLOCK_SIZE = 100_000;

  public static <T> void sort(Reader<T> source,
      Path outputPath,
      Comparator<T> comparator,
      Function<Path, SequentialWriter<T>> writerSupplier,
      Function<Path, Reader<T>> readerSupplier) {
    sort(source, outputPath, DEFAULT_READ_BLOCK_SIZE, comparator, writerSupplier, readerSupplier);
  }

  public static <T> void sort(Reader<T> source,
      Path outputPath,
      int maxSortBlockSize,
      Comparator<T> comparator,
      Function<Path, SequentialWriter<T>> writerSupplier,
      Function<Path, Reader<T>> readerSupplier) {
    Path outputRoot = outputPath.normalize().getParent();
    Path tmpOutputRoot;
    try {
      Files.createDirectories(outputRoot);
      tmpOutputRoot = Files.createTempDirectory(outputRoot, ".SorterTmp");
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("External sorting crushed: failed to create temporary directory"
              + " by the path: [ %s ]", outputRoot.toAbsolutePath().toString()), e);
    }

    Path chunksRoot;
    try {
      chunksRoot = tmpOutputRoot.resolve("Chunks");
      Files.createDirectories(chunksRoot);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("External sorting crushed: failed to create directory: [ %s ]",
              tmpOutputRoot.resolve("Chunks").toAbsolutePath().toString()), e);
    }
    List<T> objectsList;
    int chunkIndex = 0;
    while (!(objectsList = readObjects(source, maxSortBlockSize)).isEmpty()) {
      objectsList.sort(comparator);
      try (SequentialWriter<T> writer = writerSupplier.apply(chunkPath(chunksRoot, chunkIndex++))) {
        objectsList.forEach(writer::append);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // TODO: check chunks count not to be too large
    List<Reader<T>> chunkReaders = IntStream.range(0, chunkIndex)
        .mapToObj(i -> chunkPath(chunksRoot, i))
        .map(readerSupplier)
        .collect(Collectors.toList());

    Path tmpOutput = tmpOutputRoot.resolve("Output");
    try (
        SortedReaderCombiner<T> sortedReaderCombiner =
            new SortedReaderCombiner<>(chunkReaders, comparator);
        SequentialWriter<T> resultWriter = writerSupplier.apply(tmpOutput)
    ) {
      T object;
      while ((object = sortedReaderCombiner.next()) != null) {
        resultWriter.append(object);
      }
    } catch (IOException e) {
      throw new RuntimeException("External sorting crushed: failed to merge chunks", e);
    }

    try {
      FileUtils.deleteDirectory(chunksRoot.toFile());
      Files.move(tmpOutput, outputPath, StandardCopyOption.REPLACE_EXISTING);
      FileUtils.deleteDirectory(tmpOutputRoot.toFile());
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("External sorting crushed: failed to move temporary output from"
              + " the temporary path [ %s ] to the destination path [ %s ]",
              tmpOutput.toAbsolutePath().toString(), outputPath.toString()), e);
    }
  }

  private static Path chunkPath(Path root, int chunkIndex) {
    return root.resolve(String.format("chunk-%05d", chunkIndex));
  }

  private static <T> List<T> readObjects(Reader<T> source, int limit) {
    List<T> objectsList = new ArrayList<>();
    T object;
    while ((object = source.read()) != null && objectsList.size() <= limit) {
      objectsList.add(object);
    }
    return objectsList;
  }

  static class SortedReaderCombiner<T> implements Closeable {

    private final PriorityQueue<Pair<T, Reader<T>>> sortedSupplier;
    private final List<Reader<T>> readers;

    SortedReaderCombiner(List<Reader<T>> readers, Comparator<T> comparator) {
      this.readers = readers;
      sortedSupplier = new PriorityQueue<>(readers.size(), (p1, p2) ->
          comparator.compare(p1.getFirst(), p2.getFirst()));
      readers.stream()
          .map(r -> Pair.create(r.read(), r))
          .filter(p -> p.getFirst() != null)
          .forEach(sortedSupplier::add);
    }

    T next() {
      Pair<T, Reader<T>> source = null;
      while (!sortedSupplier.isEmpty() && (source = sortedSupplier.poll()).getFirst() == null) {
        ;
      }
      if (source == null) {
        return null;
      }

      T nextObject = source.getSecond().read();
      if (nextObject != null) {
        sortedSupplier.add(Pair.create(nextObject, source.getSecond()));
      }
      return source.getFirst();
    }

    @Override
    public void close() throws IOException {
      for (Reader<T> reader : readers) {
        reader.close();
      }
    }
  }
}
