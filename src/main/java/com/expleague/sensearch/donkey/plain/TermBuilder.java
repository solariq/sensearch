package com.expleague.sensearch.donkey.plain;

import static com.expleague.sensearch.donkey.utils.BrandNewIdGenerator.generateTermId;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term.Builder;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  private final Map<Long, ParsedTerm> terms = new ConcurrentHashMap<>();
  private final Map<CharSeq, ParsedTerm> termsCache = new ConcurrentHashMap<>();

  private final DB termDb;

  private final MyStem myStem;

  public TermBuilder(DB termDb, MyStem lemmer) {
    this.termDb = termDb;
    this.myStem = lemmer;
  }

  /**
   * Adds term to the builder and returns its id as well as its lemma id. Lemma id will be equal to
   * term id if lemma equals to term or lemma cannot be parsed
   */
  // TODO: do not store terms in memory as we have only write access to them
  @NotNull
  public ParsedTerm addTerm(CharSequence wordcs) {
    CharSeq word = CharSeq.create(wordcs);
    ParsedTerm parsedTerm = termsCache.get(CharSeq.create(word));
    if (parsedTerm != null) {
      return parsedTerm;
    }

    word = CharSeq.intern(word);

    LemmaInfo lemma = null;
    List<WordInfo> parse = myStem.parse(word);
    if (parse.size() > 0) {
      lemma = parse.get(0).lemma();
    }

    long wordId = generateTermId(word);

    //noinspection EqualsBetweenInconvertibleTypes
    if (lemma == null || lemma.lemma().equals(word)) {
      final ParsedTerm value =
          new ParsedTerm(
              wordId, -1, word, lemma == null ? null : PartOfSpeech.valueOf(lemma.pos().name()));
      termsCache.put(word, value);
      terms.put(wordId, value);
      return value;
    }

    long lemmaId = generateTermId(lemma.lemma());

    parsedTerm = new ParsedTerm(wordId, lemmaId, word, PartOfSpeech.valueOf(lemma.pos().name()));
    terms.put(wordId, parsedTerm);
    termsCache.put(word, parsedTerm);
    if (!terms.containsKey(lemmaId)) {
      final ParsedTerm lemmaParsed =
          new ParsedTerm(lemmaId, -1, lemma.lemma(), PartOfSpeech.valueOf(lemma.pos().name()));
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

    terms.forEach(
        (id, term) -> {
          Builder termBuilder =
              Term.newBuilder().setId(id).setText(term.text.toString()).setLemmaId(term.lemmaId);

          if (term.partOfSpeech != null) {
            termBuilder.setPartOfSpeech(
                IndexUnits.Term.PartOfSpeech.valueOf(term.partOfSpeech.name()));
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
        });

    if (curBatchSize[0] > 0) {
      termDb.write(batch[0], DEFAULT_TERM_WRITE_OPTIONS);
    }

    termDb.close();
  }

  public static class ParsedTerm {

    final long id;
    final long lemmaId;
    final CharSeq text;
    final PartOfSpeech partOfSpeech;

    public ParsedTerm(long id, long lemmaId, CharSeq text, PartOfSpeech partOfSpeech) {
      this.id = id;
      this.lemmaId = lemmaId;
      this.text = text;
      this.partOfSpeech = partOfSpeech;
    }
  }
}
