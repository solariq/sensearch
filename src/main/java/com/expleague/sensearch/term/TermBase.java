package com.expleague.sensearch.term;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.plain.IndexTerm;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class TermBase implements AutoCloseable {

  private static final Logger LOG = Logger.getLogger(TermBase.class.getName());

  private static final long DEFAULT_CACHE_SIZE = 128 * (1 << 20); // 128 MB
  private static final Options DEFAULT_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .createIfMissing(false)
          .compressionType(CompressionType.SNAPPY);

  private final DB termBase;
  private final PlainIndex index;

  private final Map<CharSeq, Term> wordToTerms = new HashMap<>();
  private final TIntObjectMap<Term> idToTerm = new TIntObjectHashMap<>();

  //TODO: delete index in Term
  public TermBase(Path termBasePath, PlainIndex index) throws IOException {
    LOG.info("Load TermBase...");
    termBase = JniDBFactory.factory.open(termBasePath.toFile(), DEFAULT_DB_OPTIONS);
    this.index = index;
    load();
    LOG.info("TermBase loaded...");
  }

  private void load() {
    DBIterator termIterator = termBase.iterator();
    termIterator.seekToFirst();
    termIterator.forEachRemaining(
        item -> {
          try {
            IndexUnits.Term protoTerm = IndexUnits.Term.parseFrom(item.getValue());
            final CharSeq word = CharSeq.intern(protoTerm.getText());

            if (wordToTerms.containsKey(word)) {
              return;
            }

            PartOfSpeech pos =
                protoTerm.getPartOfSpeech() == IndexUnits.Term.PartOfSpeech.UNKNOWN
                    ? null
                    : PartOfSpeech.valueOf(protoTerm.getPartOfSpeech().name());

            final IndexTerm lemmaTerm;

            final int lemmaId = protoTerm.getLemmaId();
            if (lemmaId == -1) {
              lemmaTerm = null;
            } else {
              if (idToTerm.containsKey(lemmaId)) {
                lemmaTerm = (IndexTerm) idToTerm.get(lemmaId);
              } else {
                CharSeq lemmaText =
                    CharSeq.intern(
                        IndexUnits.Term.parseFrom(termBase.get(Ints.toByteArray(lemmaId)))
                            .getText());

                lemmaTerm = new IndexTerm(index, lemmaText, lemmaId, null, pos);
                idToTerm.put(lemmaId, lemmaTerm);
                wordToTerms.put(lemmaText, lemmaTerm);
              }
            }

            IndexTerm term = new IndexTerm(index, word, protoTerm.getId(), lemmaTerm, pos);
            idToTerm.put(protoTerm.getId(), term);
            wordToTerms.put(word, term);

          } catch (InvalidProtocolBufferException e) {
            LOG.fatal("Invalid protobuf for term with id " + Longs.fromByteArray(item.getKey()));
            throw new RuntimeException(e);
          }
        });
  }

  public Term term(int id) {
    return idToTerm.get(id);
  }

  public Term term(CharSequence word) {
    return wordToTerms.get(CharSeq.intern(word));
  }

  public Stream<IndexUnits.Term> stream() {
    List<IndexUnits.Term> terms = new LinkedList<>();
    DBIterator termIterator = termBase.iterator();
    termIterator.seekToFirst();
    termIterator.forEachRemaining(
        item -> {
          try {
            IndexUnits.Term protoTerm = IndexUnits.Term.parseFrom(item.getValue());
            terms.add(protoTerm);
          } catch (InvalidProtocolBufferException e) {
            LOG.fatal("Invalid protobuf for term with id " + Longs.fromByteArray(item.getKey()));
            throw new RuntimeException(e);
          }
        });
    return terms.stream();
  }

  @Override
  public void close() throws IOException {
    termBase.close();
  }
}
