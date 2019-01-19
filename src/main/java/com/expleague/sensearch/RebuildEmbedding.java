package com.expleague.sensearch;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.EMBEDDING_ROOT;
import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.LSH_ROOT;
import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.VECS_ROOT;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.IdGenerator;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.Options;

public class RebuildEmbedding {

  public static void main(String[] args) throws IOException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Injector injector = Guice.createInjector(new AppModule());
    Config config = injector.getInstance(Config.class);
    Index index = injector.getInstance(Index.class);

    Embedding<CharSeq> jmllEmbedding =
        EmbeddingImpl.read(
            new FileReader(Paths.get(config.getEmbeddingVectors()).toFile()), CharSeq.class);

    final Path indexRoot = config.getTemporaryIndex();

    Files.createDirectory(indexRoot.resolve(EMBEDDING_ROOT + "_tmp"));

    try (final EmbeddingBuilder embeddingBuilder =
        new EmbeddingBuilder(
            JniDBFactory.factory.open(
                indexRoot.resolve(EMBEDDING_ROOT + "_tmp").resolve(VECS_ROOT).toFile(),
                new Options()),
            JniDBFactory.factory.open(
                indexRoot.resolve(EMBEDDING_ROOT + "_tmp").resolve(LSH_ROOT).toFile(),
                new Options()),
            indexRoot.resolve(EMBEDDING_ROOT),
            jmllEmbedding,
            new TokenizerImpl(),
            new IdGenerator())) {

      index
          .allDocuments()
          .map(doc -> (IndexedPage) doc)
          .forEach(
              doc -> {
                embeddingBuilder.startPage(doc.id());

                embeddingBuilder.add(doc.content(SegmentType.TITLE).toString());
                embeddingBuilder.add(doc.content(SegmentType.BODY).toString());

                embeddingBuilder.endPage();
              });
    }

    Files.move(indexRoot.resolve(EMBEDDING_ROOT + "_tmp").resolve(VECS_ROOT),
        indexRoot.resolve(EMBEDDING_ROOT).resolve(VECS_ROOT));
    Files.move(indexRoot.resolve(EMBEDDING_ROOT + "_tmp").resolve(LSH_ROOT),
        indexRoot.resolve(EMBEDDING_ROOT).resolve(LSH_ROOT));
  }
}
