package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.utils.ParsedTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term.PartOfSpeech;
import com.google.common.primitives.Longs;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TermWriter extends LevelDbBasedWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageWriter.class);

  public TermWriter(Path root) {
    super(root);
  }

  public void writeTerm(ParsedTerm parsedTerm) {
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
    rootDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = rootDb.createWriteBatch();
  }

}
