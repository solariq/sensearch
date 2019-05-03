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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

public class UriMappingsBuilder implements AutoCloseable, IncrementalBuilder {

  public static final String ROOT = "uriMapping";

  private static final Options DEFAULT_DB_OPTIONS = new Options()
      .createIfMissing(true)
      .errorIfExists(true)
      .compressionType(CompressionType.SNAPPY);
  private static final Logger LOG = Logger.getLogger(UriMappingsBuilder.class);

  private final TObjectLongMap<String> uriMappings = TCollections.synchronizedMap(
      new TObjectLongHashMap<>()
  );
  private final Path indexRoot;

  UriMappingsBuilder(Path indexRoot) {
    this.indexRoot = indexRoot;
  }

  void addSection(URI sectionUri, long sectionId) {
    uriMappings.put(sectionUri.toASCIIString(), sectionId);
  }

  @Override
  public void increment(BuilderState... increments) {
    for (BuilderState state : increments) {
      if (!(state instanceof UriMappingBuilderState)) {
        throw new IllegalArgumentException("One of the received states is not a state of"
            + "UriMappingsBuilder!");
      }
    }

    for (BuilderState state : increments) {
      UriMappingBuilderState uriMappingBuilderState = (UriMappingBuilderState) state;
      this.uriMappings.putAll(uriMappingBuilderState.uriMappings);
    }
  }

  @Override
  public UriMappingBuilderState state() {
    return new UriMappingBuilderState(this);
  }

  @Override
  public void resetState() {
    uriMappings.clear();
  }

  @Override
  public void setState(BuilderState state) {
    if (!(state instanceof UriMappingBuilderState)) {
      throw new IllegalArgumentException("Received state is not a state of UriMappingsBuilder!");
    }
    increment(state);
  }

  /**
   * FIXME this is actually a crutch: in case of normal execution
   * FIXME (and calling close() is a sign of it) we don't need to pass saved states
   * @param states previously saved states to be included to the builder
   * @throws IOException
   */
  public void closeWith(BuilderState... states) throws IOException {
    LOG.info("Storing URI mapping...");
    DB uriMappingDb = JniDBFactory.factory.open(indexRoot.resolve(ROOT).toFile(),
        DEFAULT_DB_OPTIONS);
    storeMappings(uriMappingDb, uriMappings);
    for (BuilderState state : states) {
      if (!(state instanceof  UriMappingBuilderState)) {
        continue;
      }
      UriMappingBuilderState localState = (UriMappingBuilderState) state;
      storeMappings(uriMappingDb, localState.mappings());
    }
    uriMappingDb.close();
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing URI mapping...");
    DB uriMappingDb = JniDBFactory.factory.open(indexRoot.resolve(ROOT).toFile(),
        DEFAULT_DB_OPTIONS);
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

    private static final String META_FILE = "meta";
    private static final String MAPPINGS_FILE_PROP = "map";

    private TObjectLongMap<String> uriMappings;
    private StateMeta meta;

    private UriMappingBuilderState(UriMappingsBuilder builder) {
      this.uriMappings = new TObjectLongHashMap<>();
      this.uriMappings.putAll(builder.uriMappings);
    }

    private UriMappingBuilderState(StateMeta meta) {
      this.meta = meta;
    }

    @SuppressWarnings("unchecked")
    private TObjectLongMap<String> mappings() {
      if (uriMappings != null) {
        return uriMappings;
      }
      Path pathToMappings = meta.getAsPath(MAPPINGS_FILE_PROP).toAbsolutePath();
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
      List<Path> metas = Files
          .find(from, 1, (p, a) -> p.endsWith(META_FILE))
          .collect(Collectors.toList());
      for (Path metaFile : metas) {
        try {
          StateMeta meta = StateMeta.readFrom(metaFile);
          if (meta.owner() == UriMappingBuilderState.class) {
            return new UriMappingBuilderState(meta);
          } else {
            LOG.warn(String.format("By given path [ %s ] was found meta file [ %s ]with"
                + " unsuitable owner", from.toString(), metaFile.toString()));
          }
        } catch (IOException e) {
          LOG.warn(String.format("Failed to parse possible meta file: [ %s ]",
              metaFile.toString()));
        }
      }

      throw new IOException(
          String.format("Could find meta files by given path [ %s ]", from.toString()));
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

      uriMappings.clear();
    }
  }
}
