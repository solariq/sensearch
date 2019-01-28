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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.jetbrains.annotations.NotNull;

public class TermBuilder implements AutoCloseable {

  private static final int TERM_BATCH_SIZE = 1024;
  private static final int CACHE_LEMMAS_NUM = 1 << 20;

  private static final Logger LOG = Logger.getLogger(TermBuilder.class);

  private static final WriteOptions DEFAULT_TERM_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final TObjectLongMap<String> idMapping = new TObjectLongHashMap<>();
  private final DB termDb;
  private final MyStem myStem;
  private final TLongObjectMap<ParsedTerm> terms = new TLongObjectHashMap<>();
  private final IdGenerator idGenerator;
  private final Map<String, LemmaInfo> cachedLemmas = new LinkedHashMap<String, LemmaInfo>() {
    @Override
    protected boolean removeEldestEntry(Entry entry) {
      return size() > CACHE_LEMMAS_NUM;
    }
  };

  public TermBuilder(DB termDb, Lemmer lemmer, IdGenerator idGenerator) {
    this.termDb = termDb;
    this.myStem = lemmer.myStem;
    this.idGenerator = idGenerator;
  }

  /**
   * Adds term to the builder and returns its id as well as its lemma id.
   * Lemma id will be equal to term id if lemma equals to term or lemma cannot be parsed
   */
  // TODO: do not store terms in memory as we have only write access to them
  @NotNull
  public TermAndLemmaIdPair addTerm(String word) {
    LemmaInfo lemma;
    if (cachedLemmas.containsKey(word)) {
      lemma = cachedLemmas.get(word);
    } else {
      final List<WordInfo> parse = myStem.parse(word);
      lemma = parse.size() > 0 ? parse.get(0).lemma() : null;
      cachedLemmas.put(word, lemma);
    }

    long wordId;
    if (!idMapping.containsKey(word)) {
      // Ids start from 1
      wordId = idGenerator.termId(word);
      idMapping.put(word, wordId);
    } else {
      wordId = idMapping.get(word);
    }

    //noinspection EqualsBetweenInconvertibleTypes
    if (lemma == null || lemma.lemma().equals(word)) {
      terms.put(
          wordId,
          new ParsedTerm(
              wordId, -1, lemma == null ? null : PartOfSpeech.valueOf(lemma.pos().name())));
      return new TermAndLemmaIdPair(wordId, wordId);
    }

    long lemmaId;

    String lemmaStr = lemma.lemma().toString();
    if (idMapping.containsKey(lemmaStr)) {
      lemmaId = idMapping.get(lemmaStr);
    } else {
      // Ids start from 1
      lemmaId = idGenerator.termId(lemmaStr);
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
            try {
              batch[0].close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
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

    final long termId;
    final long lemmaId;

    public TermAndLemmaIdPair(long termId, long lemmaId) {
      this.termId = termId;
      this.lemmaId = lemmaId;
    }
  }

  static class ParsedTerm {

    final long id;
    final long lemmaId;
    final PartOfSpeech partOfSpeech;

    public ParsedTerm(long id, long lemmaId, PartOfSpeech partOfSpeech) {
      this.id = id;
      this.lemmaId = lemmaId;
      this.partOfSpeech = partOfSpeech;
    }
  }
}
