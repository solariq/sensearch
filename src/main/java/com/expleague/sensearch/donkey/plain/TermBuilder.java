package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.stem.Stemmer;
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
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.jetbrains.annotations.NotNull;

import static com.expleague.sensearch.donkey.utils.BrandNewIdGenerator.termIdGenerator;

public class TermBuilder implements AutoCloseable {

  private static final int TERM_BATCH_SIZE = 1024;
  private static final int CACHE_LEMMAS_NUM = 1 << 20;

  private static final Logger LOG = Logger.getLogger(TermBuilder.class);

  private static final WriteOptions DEFAULT_TERM_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final TObjectLongMap<CharSeq> idMapping = new TObjectLongHashMap<>();
  private final DB termDb;
  private final MyStem myStem;
  private final TLongObjectMap<ParsedTerm> terms = new TLongObjectHashMap<>();
  private final Map<CharSeq, ParsedTerm> termsCache = new HashMap<>();
  private final Map<CharSeq, LemmaInfo> cachedLemmas = new LinkedHashMap<CharSeq, LemmaInfo>() {
    @Override
    protected boolean removeEldestEntry(Entry entry) {
      return size() > CACHE_LEMMAS_NUM;
    }
  };

  private final TLongSet knownTermIds = new TLongHashSet();

  public TermBuilder(DB termDb, Lemmer lemmer) {
    this.termDb = termDb;
    this.myStem = lemmer.myStem;
  }

  /**
   * Adds term to the builder and returns its id as well as its lemma id.
   * Lemma id will be equal to term id if lemma equals to term or lemma cannot be parsed
   */
  // TODO: do not store terms in memory as we have only write access to them
  @NotNull
  public ParsedTerm addTerm(CharSequence wordcs) {
    CharSeq word = CharSeq.create(wordcs);
    ParsedTerm parsedTerm = termsCache.get(CharSeq.create(word));
    if (parsedTerm != null)
      return parsedTerm;

    word = CharSeq.intern(word);
    LemmaInfo lemma = cachedLemmas.get(word);
    if (lemma == null) {
      /*final List<WordInfo> parse = myStem.parse(word);
      lemma = parse.size() > 0 ? parse.get(0).lemma() : null;*/
      lemma = new LemmaInfo(CharSeq.create(Stemmer.getInstance().stem(word)), 0, com.expleague.commons.text.lemmer.PartOfSpeech.S);
      cachedLemmas.put(CharSeq.intern(word), lemma);
    }

    long wordId = idMapping.get(word);
    if (wordId == idMapping.getNoEntryValue()) {
      // Ids start from 1
      wordId = termIdGenerator(word).next(knownTermIds);
      idMapping.put(word, wordId);
    }

    //noinspection EqualsBetweenInconvertibleTypes
    if (lemma == null || lemma.lemma().equals(word)) {
      final ParsedTerm value = new ParsedTerm(wordId, -1, lemma == null ? null : PartOfSpeech.valueOf(lemma.pos().name()));
      termsCache.put(word, value);
      terms.put(wordId, value);
      return value;
    }

    long lemmaId = idMapping.get(lemma.lemma());
    if (lemmaId == idMapping.getNoEntryValue()) {
      // Ids start from 1
      lemmaId = termIdGenerator(lemma.lemma()).next(knownTermIds);
      idMapping.put(lemma.lemma(), lemmaId);
    }

    parsedTerm = new ParsedTerm(wordId, lemmaId, PartOfSpeech.valueOf(lemma.pos().name()));
    terms.put(wordId, parsedTerm);
    termsCache.put(word, parsedTerm);
    if (!terms.containsKey(lemmaId)) {
      final ParsedTerm lemmaParsed = new ParsedTerm(lemmaId, -1, PartOfSpeech.valueOf(lemma.pos().name()));
      terms.put(lemmaId, lemmaParsed);
      termsCache.put(lemma.lemma(), lemmaParsed);
    }

    return parsedTerm;
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing term-wise data...");

    WriteBatch[] batch = new WriteBatch[]{termDb.createWriteBatch()};
    int[] curBatchSize = new int[]{0};

    idMapping.forEachEntry(
        (word, id) -> {
          Builder termBuilder = Term.newBuilder().setId(id).setText(word.toString());

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

  public static class ParsedTerm {

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
