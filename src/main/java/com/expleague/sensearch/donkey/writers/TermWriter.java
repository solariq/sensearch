package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.term.ParsedTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term.PartOfSpeech;
import com.google.common.primitives.Longs;

import java.io.IOException;
import java.nio.file.Path;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermWriter implements Writer<ParsedTerm> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageWriter.class);
  private static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);
  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
      .sync(true);
  private static final int BATCH_SIZE = 1000;

  private final DB termsDb;
  private final Path root;
  private WriteBatch writeBatch;
  private int curBatchSize;

  public TermWriter(Path outputPath) {
    this.root = outputPath;
    try {
      termsDb = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
      writeBatch = termsDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("ConstantConditions")
  public void write(ParsedTerm parsedTerm) {
    Term.Builder builder = Term.newBuilder();

    if (parsedTerm.hasLemma()) {
      builder.setText(parsedTerm.lemma().toString());
      builder.setId(parsedTerm.lemmaId());
      builder.setPartOfSpeech(PartOfSpeech.UNKNOWN);
      builder.setLemmaId(parsedTerm.lemmaId());
      writeBatch.put(Longs.toByteArray(parsedTerm.lemmaId()), builder.build().toByteArray());
    }

    builder.clear();
    builder.setText(parsedTerm.word().toString());
    builder.setId(parsedTerm.wordId());
    builder.setPartOfSpeech(parsedTerm.hasPosTag() ?
        Term.PartOfSpeech.valueOf(parsedTerm.posTag().name()) :
        PartOfSpeech.UNKNOWN);
    builder.setLemmaId(parsedTerm.lemmaId());
    writeBatch.put(Longs.toByteArray(parsedTerm.wordId()), builder.build().toByteArray());
    if (curBatchSize >= BATCH_SIZE) {
      try {
        flush();
      } catch (IOException e) {
        LOGGER.error(e.getMessage());
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void close() throws IOException {
    flush();
    termsDb.close();
  }

  @Override
  public void flush() throws IOException {
    termsDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch.close();
    curBatchSize = 0;
    writeBatch = termsDb.createWriteBatch();
  }
}
