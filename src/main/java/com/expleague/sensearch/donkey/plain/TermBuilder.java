package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term.Builder;
import com.google.common.primitives.Longs;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.util.List;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.jetbrains.annotations.NotNull;

public class TermBuilder implements AutoCloseable {

  private static final int TERM_BATCH_SIZE = 1024;
  private static final Logger LOG = Logger.getLogger(TermBuilder.class);

  private static final WriteOptions DEFAULT_TERM_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final TObjectLongMap<String> idMapping = new TObjectLongHashMap<>();
  private final DB termDb;
  private final MyStem myStem;
  private final TLongObjectMap<ParsedTerm> terms = new TLongObjectHashMap<>();

  public TermBuilder(DB termDb, Lemmer lemmer) {
    this.termDb = termDb;
    this.myStem = lemmer.myStem;
  }

  /**
   * Adds term to the builder and returns its id as well as its lemma id. If lemma can not be parsed
   * for this word or if lemma is the same word, lemmaId equals to -1. In all other cases, ids are
   * strictly greater than 0
   */
  @NotNull
  public TermAndLemmaIdPair addTerm(String word) {
    final List<WordInfo> parse = myStem.parse(word);
    final LemmaInfo lemma = parse.size() > 0 ? parse.get(0).lemma() : null;

    long wordId;
    if (!idMapping.containsKey(word)) {
      // Ids start from 1
      wordId = idMapping.size() + 1;
      idMapping.put(word, wordId);
    } else {
      wordId = idMapping.get(word);
    }

    if (lemma == null || lemma.lemma().equals(word)) {
      terms.put(
          wordId,
          new ParsedTerm(
              wordId, -1, lemma == null ? null : PartOfSpeech.valueOf(lemma.pos().name())));
      return new TermAndLemmaIdPair(wordId, -1);
    }

    long lemmaId;

    String lemmaStr = lemma.lemma().toString();
    if (idMapping.containsKey(lemmaStr)) {
      lemmaId = idMapping.get(lemmaStr);
    } else {
      // Ids start from 1
      lemmaId = idMapping.size() + 1;
      idMapping.put(lemma.lemma().toString(), lemmaId);
    }

    terms.put(wordId, new ParsedTerm(wordId, lemmaId, PartOfSpeech.valueOf(lemma.pos().name())));
    if (!terms.containsKey(lemmaId)) {
      terms.put(lemmaId, new ParsedTerm(lemmaId, -1, PartOfSpeech.valueOf(lemma.pos().name())));
    }

    return new TermAndLemmaIdPair(wordId, lemmaId);
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing term-wise data...");

    WriteBatch[] batch = new WriteBatch[]{termDb.createWriteBatch()};
    int[] curBatchSize = new int[]{0};

    idMapping.forEachEntry(
        (word, id) -> {
          Builder termBuilder = Term.newBuilder().setId(id).setText(word);

          ParsedTerm term = terms.get(id);
          if (term != null) {
            termBuilder.setLemmaId(term.lemmaId);

            if (term.partOfSpeech != null) {
              termBuilder.setPartOfSpeech(
                  IndexUnits.Term.PartOfSpeech.valueOf(term.partOfSpeech.name()));
            }
          }

          batch[0].put(Longs.toByteArray(id), termBuilder.build().toByteArray());
          curBatchSize[0]++;
          if (curBatchSize[0] >= TERM_BATCH_SIZE) {
            termDb.write(batch[0], DEFAULT_TERM_WRITE_OPTIONS);

            batch[0] = termDb.createWriteBatch();
            curBatchSize[0] = 0;
          }

          return true;
        });

    if (curBatchSize[0] > 0) {
      termDb.write(batch[0], DEFAULT_TERM_WRITE_OPTIONS);
    }

    termDb.close();
  }

  public static class TermAndLemmaIdPair {

    long termId;
    long lemmaId;

    public TermAndLemmaIdPair(long termId, long lemmaId) {
      this.termId = termId;
      this.lemmaId = lemmaId;
    }
  }

  static class ParsedTerm {

    long id;
    long lemmaId;
    PartOfSpeech partOfSpeech;

    public ParsedTerm(long id, long lemmaId, PartOfSpeech partOfSpeech) {
      this.id = id;
      this.lemmaId = lemmaId;
      this.partOfSpeech = partOfSpeech;
    }
  }
}
