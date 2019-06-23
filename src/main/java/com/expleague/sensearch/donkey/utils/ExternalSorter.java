package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.donkey.readers.Reader;
import com.expleague.sensearch.donkey.writers.sequential.SequentialWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;

// TODO: make thread safe
public class ExternalSorter<T> {

  private static final int DEFAULT_READ_BLOCK_SIZE = 100_000;

  public static <T> void sort(Reader<T> source,
      Comparator<T> comparator,
      Function<Path, SequentialWriter<T>> writerSupplier,
      Function<Path, Reader<T>> readerSupplier,
      Path outputRoot) throws IOException {
    Path tmpChunksPath = Files.createTempDirectory(outputRoot, "TmpChunks");
    List<T> objectsList;
    int chunkIndex = 0;
    while (!(objectsList = readObjects(source, DEFAULT_READ_BLOCK_SIZE)).isEmpty()) {
      objectsList.sort(comparator);
      try (SequentialWriter<T> writer = writerSupplier
          .apply(chunkPath(tmpChunksPath, chunkIndex++))) {
        objectsList.forEach(writer::append);
      }
    }

    // TODO: check chunks count not to be too large
    List<Reader<T>> chunkReaders = IntStream.range(0, chunkIndex)
        .mapToObj(i -> chunkPath(tmpChunksPath, i))
        .map(readerSupplier)
        .collect(Collectors.toList());

    Path tmpOutput = randomPath(outputRoot);
    try (
        SortedReaderCombiner<T> sortedReaderCombiner =
            new SortedReaderCombiner<>(chunkReaders, comparator);
        SequentialWriter<T> resultWriter = writerSupplier.apply(tmpOutput)
    ) {
      T object;
      while ((object = sortedReaderCombiner.next()) != null) {
        resultWriter.append(object);
      }
    }
    FileUtils.deleteDirectory(tmpChunksPath.toFile());
    tmpOutput.toFile().renameTo(outputRoot.toFile());
  }

  private static Path randomPath(Path root) {
    Random random = new Random();
    long pathName = random.nextLong();
    while (Files.exists(root.resolve(Long.toString(pathName)))) {
      pathName = random.nextLong();
    }
    return root.resolve(Long.toString(pathName));
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
