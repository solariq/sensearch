package com.expleague.sensearch;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.IdGenerator;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import javax.xml.stream.XMLStreamException;
import org.apache.log4j.PropertyConfigurator;

public class RebuildEmbedding {

  public static void main(String[] args) throws IOException, XMLStreamException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Injector injector = Guice.createInjector(new AppModule());
    Config config = injector.getInstance(Config.class);
    Index index = injector.getInstance(Index.class);

    Embedding<CharSeq> jmllEmbedding =
        EmbeddingImpl.read(
            new FileReader(Paths.get(config.getEmbeddingVectors()).toFile()), CharSeq.class);

    EmbeddingBuilder embeddingBuilder =
        new EmbeddingBuilder(
            Paths.get(PlainIndexBuilder.EMBEDDING_ROOT), jmllEmbedding, new TokenizerImpl(),
            new IdGenerator());

    index.allDocuments().map(doc -> (IndexedPage) doc).forEach(doc -> {
      embeddingBuilder.startPage(doc.id());

      embeddingBuilder.add(doc.content(SegmentType.TITLE).toString());
      embeddingBuilder.add(doc.content(SegmentType.BODY).toString());
    });
  }
}
