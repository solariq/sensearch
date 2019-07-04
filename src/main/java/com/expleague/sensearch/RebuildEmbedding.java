package com.expleague.sensearch;

import static com.expleague.sensearch.donkey.plain.PlainIndexCreator.EMBEDDING_ROOT;
import static com.expleague.sensearch.donkey.plain.PlainIndexCreator.VECS_ROOT;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.core.Annotations.EmbeddingVectorsPath;
import com.expleague.sensearch.core.Annotations.IndexRoot;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.Options;

public class RebuildEmbedding {

  private final Path embeddingVectorsPath;
  private final Path indexRoot;
  private final Index index;

  @Inject
  public RebuildEmbedding(
      @EmbeddingVectorsPath Path embeddingVectorsPath, @IndexRoot Path indexRoot, Index index) {
    this.embeddingVectorsPath = embeddingVectorsPath;
    this.indexRoot = indexRoot;
    this.index = index;
  }

  public void rebuild() throws IOException {
    Embedding<CharSeq> jmllEmbedding =
        EmbeddingImpl.read(Files.newBufferedReader(embeddingVectorsPath), CharSeq.class);

    if (!Files.exists(indexRoot.resolve(EMBEDDING_ROOT + "_tmp"))) {
      Files.createDirectory(indexRoot.resolve(EMBEDDING_ROOT + "_tmp"));
    }

    Path vecTmpDir = indexRoot.resolve(EMBEDDING_ROOT + "_tmp").resolve(VECS_ROOT);

    try (final EmbeddingBuilder embeddingBuilder =
        new EmbeddingBuilder(
            JniDBFactory.factory.open(vecTmpDir.toFile(), new Options()),
            jmllEmbedding,
            new TokenizerImpl())) {
      index
          .allDocuments()
          .map(doc -> (IndexedPage) doc)
          .forEach(
              doc -> {
                embeddingBuilder.startPage(doc.id());
                embeddingBuilder.endPage();
              });
    }

    Path vecDbDir = indexRoot.resolve(EMBEDDING_ROOT).resolve(VECS_ROOT);
    FileUtils.deleteDirectory(vecDbDir.toFile());
    FileUtils.moveDirectory(vecTmpDir.toFile(), vecDbDir.toFile());
    FileUtils.deleteDirectory(vecTmpDir.toFile());
  }

  public static void main(String[] args) throws IOException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);

    Injector injector = Guice.createInjector(new AppModule(config));

    RebuildEmbedding rebuildEmbedding = injector.getInstance(RebuildEmbedding.class);
    rebuildEmbedding.rebuild();
  }
}
