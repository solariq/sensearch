package com.expleague.sensearch.donkey;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RecoverableBuilder {

  interface BuilderState {

    String META_FILE = "meta";

    static <T> List<T> loadProtobufList(StateMeta meta, String property, Path root,
        Class<T> clazz) {
      if (!meta.hasProperty(property)) {
        throw new IllegalStateException(
            String.format("Meta file does not have a property with the name [ %s ]", property));
      }
      Method parserMethod;
      try {
        parserMethod = clazz.getMethod("parseDelimitedFrom", InputStream.class);
      } catch (NoSuchMethodException | SecurityException e) {
        throw new IllegalStateException(
            String.format("String given class [ %s ] is not a protobuf class",
                clazz.getSimpleName()), e);
      }
      Path protobufListFile = root.resolve(meta.get(property)).toAbsolutePath();
      List<T> protobufList = new ArrayList<>();
      try (InputStream is = Files.newInputStream(protobufListFile)) {
        protobufList.add(clazz.cast(parserMethod.invoke(null, is)));
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to read terms from file [ %s ]", protobufListFile.toString()), e);
      } catch (ClassCastException | IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(
            String.format("Failed to read protobufs with the type [ %s ] from the file [ %s ]."
                    + " Probably recovery point is corrupted",
                clazz.getSimpleName(), protobufListFile.toString()), e);
      }
      return protobufList;
    }

    static <T> T loadObject(StateMeta meta, String property, Path root,
        Class<T> clazz) {
      if (!meta.hasProperty(property)) {
        throw new IllegalStateException(
            String.format("Meta file does not have a property with the name [ %s ]", property));
      }

      Path pathToObject = root.resolve(meta.get(property)).toAbsolutePath();
      try {
        ObjectInputStream deserializer = new ObjectInputStream(
            Files.newInputStream(pathToObject));
        Object object = deserializer.readObject();
        return clazz.cast(object);
      } catch (ClassCastException e) {
        throw new IllegalStateException(
            String.format("Failed to read an object of the type [ %s ] from path [ %s ]."
                    + " Probably recovery point is corrupted",
                clazz.getSimpleName(), pathToObject.toString()), e);
      } catch (IOException e) {
        throw new IllegalStateException(
            String.format("Failed to read an object from the path [ %s ]",
                pathToObject.toString()), e
        );
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            String.format("Path [ %s ] does not contain an object!",
                pathToObject.toString()), e
        );
      }
    }

    static BuilderState loadFrom(Path from, Class<? extends BuilderState> stateClass,
        Logger logger) throws IOException {
      List<Path> metas = Files
          .find(from, 1, (p, a) -> p.endsWith(META_FILE))
          .collect(Collectors.toList());
      for (Path metaFile : metas) {
        try {
          StateMeta meta = StateMeta.readFrom(metaFile);
          if (meta.owner() == stateClass) {
            for (final Constructor<?> constructor : stateClass.getConstructors()) {
              final Class<?>[] params = constructor.getParameterTypes();
              if (params.length != 2) {
                continue;
              }
              if (!(params[0].isAssignableFrom(Path.class)
                  && params[1].isAssignableFrom(StateMeta.class))) {
                continue;
              }
              try {
                constructor.setAccessible(true);
                return (BuilderState) constructor.newInstance(from, meta);
              } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
              }
            }
          } else {
            logger.warn(String.format("By given path [ %s ] was found meta file [ %s ]with"
                + " unsuitable owner", from.toString(), metaFile.toString()));
          }
        } catch (IOException e) {
          logger.warn(String.format("Failed to parse possible meta file: [ %s ]",
              metaFile.toString()));
        }
      }

      throw new IOException(
          String.format("Could find meta files by given path [ %s ]", from.toString()));
    }

    class StateMeta {

      private static final Logger LOG = LoggerFactory.getLogger(StateMeta.class);
      private static final Gson GSON = new Gson();

      private final Class<? extends BuilderState> owner;
      private final long timestamp;
      private final Map<String, String> properties;

      public static StateMetaBuilder builder(Class<? extends BuilderState> owner, long timestamp) {
        return new StateMetaBuilder(owner, timestamp);
      }

      public static StateMetaBuilder builder(Class<? extends BuilderState> owner) {
        return builder(owner, 0);
      }

      public static StateMeta readFrom(Path from) throws IOException {
        if (Files.notExists(from)) {
          throw new IOException(
              String.format("Failed to read meta from path [ %s ]: path does not exist!",
                  from.toString())
          );
        }

        try {
          return GSON.fromJson(Files.newBufferedReader(from), StateMeta.class);
        } catch (JsonSyntaxException | JsonIOException e) {
          throw new IOException(
              String.format("Failed to read meta from file [ %s ]", from.toString()), e);
        }
      }

      private StateMeta(Class<? extends BuilderState> owner, long timestamp,
          Map<String, String> properties) {
        this.owner = owner;
        this.timestamp = timestamp;
        this.properties = properties;
      }

      public Class<? extends BuilderState> owner() {
        return owner;
      }

      public long timestamp() {
        return timestamp;
      }

      public boolean hasProperty(String propertyName) {
        return properties.containsKey(propertyName);
      }

      public String get(String propertyName) {
        if (!hasProperty(propertyName)) {
          throw new IllegalArgumentException(
              String.format("State meta has no property with name [ %s ]", propertyName));
        }

        return properties.get(propertyName);
      }

      public String[] get(String propertyName, String delimiter) {
        return get(propertyName).split(delimiter);
      }

      public String toJson() {
        return GSON.toJson(this);
      }

      public void writeTo(Path to) throws IOException {
        if (Files.exists(to)) {
          throw new IOException(
              String.format("Failed to write meta file by path [ %s ]: path already exists!",
                  to.toString())
          );
        }

        Writer writer = Files.newBufferedWriter(to);
        writer.write(toJson());
        writer.close();
      }

      public static class StateMetaBuilder {

        private Class<? extends BuilderState> owner;
        private long timestamp;
        private Map<String, String> properties = new HashMap<>();

        private StateMetaBuilder(Class<? extends BuilderState> owner, long timestamp) {
          this.owner = owner;
          this.timestamp = timestamp;
        }

        public StateMetaBuilder addProperty(String propertyName, String value) {
          if (properties.containsKey(propertyName)) {
            LOG.warn(
                String.format("State meta already has property [ %s ]. It will be overwritten!",
                    propertyName));
          }

          properties.put(propertyName, value);
          return this;
        }

        public StateMetaBuilder addProperty(String propertyName, String delimiter,
            String... values) {
          return addProperty(propertyName, String.join(delimiter, values));
        }

        public StateMeta build() {
          return new StateMeta(owner, timestamp, properties);
        }
      }
    }

    void saveTo(Path to) throws IOException;
  }

  BuilderState state();

  void setState(BuilderState state);
}
