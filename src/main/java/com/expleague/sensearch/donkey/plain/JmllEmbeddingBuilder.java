package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.decomp.DecompBuilder;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class JmllEmbeddingBuilder {

  private static final Logger LOG = Logger.getLogger(JmllEmbeddingBuilder.class);

  private final int vecSize;
  private final Path tempEmbeddingPath;

  public JmllEmbeddingBuilder(int vecSize, Path tempEmbeddingPath) {
    this.vecSize = vecSize;
    this.tempEmbeddingPath = tempEmbeddingPath;
  }

  public Embedding<CharSeq> build(Stream<CrawlerDocument> documents) throws IOException {
    Files.createDirectories(tempEmbeddingPath);
    Path corpus = tempEmbeddingPath.resolve("corpus");

    try (Writer to = new OutputStreamWriter(new FileOutputStream(corpus.toFile()))) {
      documents.forEach(doc -> {
        try {
          String title = doc.title();
          for (char c : title.toCharArray()) {
            to.write(filtrateChar(c));
          }
          to.write(' ');
          CharSequence content = doc.content();
          for (int i = 0; i < content.length(); i++) {
            to.write(filtrateChar(content.charAt(i)));
          }
          to.write(' ');
        } catch (IOException e) {
          LOG.warn(e);
        }

      });
    }

    DecompBuilder builder = (DecompBuilder) Embedding.builder(Embedding.Type.DECOMP);
    final Embedding<CharSeq> result = builder.dimSym(vecSize).file(corpus).build();
    FileUtils.deleteDirectory(tempEmbeddingPath.toFile());

    return result;
  }

  private char filtrateChar(char c) {
    if (Character.isLetter(c)) {
      return Character.toLowerCase(c);
    } else if (Character.isDigit(c)) {
      return c;
    }
    return ' ';
  }

}
