package com.expleague.sensearch;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.PropertyConfigurator;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.EMBEDDING_ROOT;
import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.VECS_ROOT;

public class RebuildEmbedding {
  public static void main(String[] args) throws IOException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Injector injector = Guice.createInjector(new AppModule());
    Config config = injector.getInstance(Config.class);
    Index index = injector.getInstance(Index.class);

    Embedding<CharSeq> jmllEmbedding =
        EmbeddingImpl.read(Files.newBufferedReader(config.getEmbeddingVectors()), CharSeq.class);

    final Path indexRoot = config.getIndexRoot();

    if (!Files.exists(indexRoot.resolve(EMBEDDING_ROOT + "_tmp"))) {
      Files.createDirectory(indexRoot.resolve(EMBEDDING_ROOT + "_tmp"));
    }

    Path vecTmpDir = indexRoot.resolve(EMBEDDING_ROOT + "_tmp").resolve(VECS_ROOT);

    try (final EmbeddingBuilder embeddingBuilder =
        new EmbeddingBuilder(
            JniDBFactory.factory.open(vecTmpDir.toFile(), new Options()),
            jmllEmbedding,
            new TokenizerImpl())) {
      index.allDocuments()
          .map(doc -> (IndexedPage) doc)
          .forEach(
              doc -> {
                embeddingBuilder.startPage(doc.id(), doc.id());
                embeddingBuilder.endPage();
              });
    }

    Path vecDbDir = indexRoot.resolve(EMBEDDING_ROOT).resolve(VECS_ROOT);
    Files.delete(vecDbDir);
    Files.move(vecTmpDir, vecDbDir);
//    Files.walk(vecTmpDir).sorted(Comparator.reverseOrder()).forEach(Functions.rethrow(Files::delete));

    /*File lshDbDir = indexRoot.resolve(EMBEDDING_ROOT).resolve(LSH_ROOT).toFile();
    FileUtils.deleteDirectory(lshDbDir);
    FileUtils.moveDirectory(lshTmpDir, lshDbDir);
    FileUtils.deleteDirectory(lshTmpDir);*/
  }
}
