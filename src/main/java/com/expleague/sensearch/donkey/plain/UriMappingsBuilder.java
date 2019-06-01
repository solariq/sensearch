package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.IncrementalBuilder;
import com.expleague.sensearch.protobuf.index.IndexUnits.UriPageMapping;
import gnu.trove.TCollections;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UriMappingsBuilder implements AutoCloseable, IncrementalBuilder {

  private static final Options DEFAULT_DB_OPTIONS = new Options()
      .createIfMissing(true)
      .errorIfExists(true)
      .compressionType(CompressionType.SNAPPY);
  private static final Logger LOG = LoggerFactory.getLogger(UriMappingsBuilder.class);

  private final TObjectLongMap<String> uriMappings = TCollections.synchronizedMap(
      new TObjectLongHashMap<>()
  );
  private final Path uriMappingRoot;
  private final List<UriMappingBuilderState> priorStates = new ArrayList<>();

  UriMappingsBuilder(Path uriMappingRoot) {
    this.uriMappingRoot = uriMappingRoot;
  }

  void addSection(URI sectionUri, long sectionId) {
    uriMappings.put(sectionUri.toASCIIString(), sectionId);
  }

  @Override
  public synchronized void setStates(BuilderState... increments) {
    resetState();
    priorStates.clear();
    priorStates.addAll(IncrementalBuilder.accumulate(UriMappingBuilderState.class, increments));
  }

  @Override
  public synchronized BuilderState state() {
    UriMappingBuilderState state = new UriMappingBuilderState(this);
    priorStates.add(state);
    resetState();
    return state;
  }

  private synchronized void resetState() {
    uriMappings.clear();
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing URI mapping...");
    DB uriMappingDb = JniDBFactory.factory.open(uriMappingRoot.toFile(),
        DEFAULT_DB_OPTIONS);
    if (!priorStates.isEmpty()) {
      for (UriMappingBuilderState state : priorStates) {
        storeMappings(uriMappingDb, state.mappings());
      }
    }
    storeMappings(uriMappingDb, uriMappings);
    uriMappingDb.close();
  }

  private static void storeMappings(DB dataBase, TObjectLongMap<String> mappings) {
    final WriteBatch[] writeBatch = new WriteBatch[]{dataBase.createWriteBatch()};
    int maxPagesInBatch = 1000;
    int[] pagesInBatch = new int[1];

    mappings.forEachEntry(
        (uri, id) -> {
          writeBatch[0].put(
              uri.getBytes(),
              UriPageMapping.newBuilder().setUri(uri).setPageId(id).build().toByteArray());
          pagesInBatch[0]++;
          if (pagesInBatch[0] >= maxPagesInBatch) {
            dataBase.write(writeBatch[0]);
            pagesInBatch[0] = 0;
            try {
              writeBatch[0].close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            writeBatch[0] = dataBase.createWriteBatch();
          }
          return true;
        });
    if (pagesInBatch[0] > 0) {
      dataBase.write(writeBatch[0]);
    }
  }

  static class UriMappingBuilderState implements BuilderState {
    private static final String MAPPINGS_FILE_PROP = "map";

    private TObjectLongMap<String> uriMappings;
    private Path root;
    private StateMeta meta;

    private UriMappingBuilderState(UriMappingsBuilder builder) {
      this.uriMappings = new TObjectLongHashMap<>();
      this.uriMappings.putAll(builder.uriMappings);
    }

    private UriMappingBuilderState(Path root, StateMeta meta) {
      this.root = root;
      this.meta = meta;
    }

    @SuppressWarnings("unchecked")
    private TObjectLongMap<String> mappings() {
      if (uriMappings != null) {
        return uriMappings;
      }
      Path pathToMappings = root.resolve(meta.get(MAPPINGS_FILE_PROP)).toAbsolutePath();
      try {
        ObjectInputStream deserializer = new ObjectInputStream(
            Files.newInputStream(pathToMappings));
        Object object = deserializer.readObject();
        if (!(object instanceof TObjectLongMap)) {
          throw new IllegalStateException("Encountered corrupted recovery point!");
        }
        uriMappings = (TObjectLongMap<String>) object;
        return uriMappings;
      } catch (IOException e) {
        throw new IllegalStateException(
            String.format("Failed to read mappings from the path [ %s ]",
                pathToMappings.toString())
        );
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            String.format("Path [ %s ] does not contain uri mappings!",
                pathToMappings.toString())
        );
      }
    }

    public static BuilderState loadFrom(Path from) throws IOException {
      return BuilderState.loadFrom(from, UriMappingBuilderState.class, LOG);
    }

    @Override
    public void saveTo(Path to) throws IOException {
      if (Files.exists(to)) {
        throw new IOException(String.format("Path [ %s ] already exists!", to.toString()));
      }

      Files.createDirectories(to);
      String mappingFileName = "mappings.map";
      meta = StateMeta.builder(UriMappingBuilderState.class)
          .addProperty(MAPPINGS_FILE_PROP, mappingFileName)
          .build();

      meta.writeTo(to.resolve(META_FILE));
      ObjectOutputStream serializer = new ObjectOutputStream(
          Files.newOutputStream(to.resolve(mappingFileName))
      );
      serializer.writeObject(uriMappings);
      serializer.close();

      root = to;
      uriMappings = null;
    }
  }
}
