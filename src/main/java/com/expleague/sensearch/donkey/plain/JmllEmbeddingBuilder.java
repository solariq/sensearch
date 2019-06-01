package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.Embedding.WindowType;
import com.expleague.ml.embedding.decomp.DecompBuilder;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
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

    if (!Files.exists(corpus)) {
      LOG.info("Creating corpus for embedding...");
      try (Writer to = Files.newBufferedWriter(corpus, StandardCharsets.UTF_8)) {
        final int[] cnt = {0};
        documents.forEach(doc -> {
          if (cnt[0] % 10_000 == 0) {
            LOG.info("Write " + cnt[0] + " lines to corpus");
          }
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
            to.write('\n');
            cnt[0]++;
          } catch (IOException e) {
            LOG.warn(e);
          }
        });
      }
    }
    else LOG.info("Using existing corpus.");

    LOG.info("Training embedding...");

    DecompBuilder builder = (DecompBuilder) Embedding.builder(Embedding.Type.DECOMP);
    final Embedding<CharSeq> result = builder.dimSym(vecSize).dimSkew(20).minWordCount(80).step(0.05)
        .window(WindowType.LINEAR, 15, 15).file(corpus).build();

    LOG.info("Jmll embedding trained");
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
