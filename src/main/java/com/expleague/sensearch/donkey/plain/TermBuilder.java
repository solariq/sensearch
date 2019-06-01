package com.expleague.sensearch.donkey.plain;


import com.expleague.sensearch.donkey.IncrementalBuilder;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder.ParsedTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term.PartOfSpeech;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermBuilder implements AutoCloseable, IncrementalBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(TermBuilder.class);
  private static final int TERM_BATCH_SIZE = 1 << 20;
  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private static final long DEFAULT_CACHE_SIZE = 1 << 20;
  private static final int DEFAULT_BLOCK_SIZE = 1 << 20;
  private static final Options TERM_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockSize(DEFAULT_BLOCK_SIZE) // 1 MB
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private final Queue<Term> terms = new ConcurrentLinkedDeque<>();
  private final Path termBaseRoot;

  private final List<TermBuilderState> priorStates = new ArrayList<>();
  TermBuilder(Path termBaseRoot) {
    this.termBaseRoot = termBaseRoot;
  }

  /**
   * Adds term to the builder and returns its id as well as its lemma id. Lemma id will be equal to
   * term id if lemma equals to term or lemma cannot be parsed
   */
  void addTerm(ParsedTerm parsedTerm) {
    Term protoTerm = Term.newBuilder()
        .setId(parsedTerm.wordId())
        .setText(parsedTerm.word())
        .setLemmaId(parsedTerm.lemmaId())
        .setPartOfSpeech(
            parsedTerm.posTag() != null ? Term.PartOfSpeech.valueOf(parsedTerm.posTag().name()) :
                PartOfSpeech.UNKNOWN
        )
        .build();
    terms.add(protoTerm);

    if (!parsedTerm.hasLemma()) {
      return;
    }

    Term protoLemma = Term.newBuilder()
        .setId(parsedTerm.lemmaId())
        .setText(parsedTerm.lemma())
        .setPartOfSpeech(
            parsedTerm.posTag() != null ? Term.PartOfSpeech.valueOf(parsedTerm.posTag().name()) :
                PartOfSpeech.UNKNOWN
        )
        .build();
    terms.add(protoLemma);
  }

  @Override
  public void close() throws IOException {
    LOG.info("Storing term-wise data...");

    DB termDb = JniDBFactory.factory.open(termBaseRoot.toFile(), TERM_DB_OPTIONS);
    for (TermBuilderState state : priorStates) {
      writeTerms(state.terms(), termDb);
    }
    writeTerms(terms, termDb);
    termDb.close();
  }

  private static void writeTerms(Iterable<Term> terms, DB termDb) throws IOException {
    WriteBatch[] batch = new WriteBatch[]{termDb.createWriteBatch()};
    int[] curBatchSize = new int[]{0};

    terms.forEach(
        t -> {
          batch[0].put(Longs.toByteArray(t.getId()), t.toByteArray());
          curBatchSize[0]++;
          if (curBatchSize[0] >= TERM_BATCH_SIZE) {
            termDb.write(batch[0], DEFAULT_WRITE_OPTIONS);
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
      termDb.write(batch[0], DEFAULT_WRITE_OPTIONS);
    }
  }

  @Override
  public synchronized void setStates(BuilderState... increments) {
    resetState();
    priorStates.clear();
    priorStates.addAll(IncrementalBuilder.accumulate(TermBuilderState.class, increments));
  }

  @Override
  public synchronized BuilderState state() {
    TermBuilderState state = new TermBuilderState(this);
    priorStates.add(state);
    resetState();
    return state;
  }

  private synchronized void resetState() {
    terms.clear();
  }

  static final class TermBuilderState implements BuilderState {
    private static final Logger LOG = LoggerFactory.getLogger(TermBuilderState.class);
    private static final String TERMS_FILE_PROP = "tlist";

    private Path root = null;
    private StateMeta meta = null;
    private List<Term> terms = null;

    private TermBuilderState(TermBuilder builder) {
      terms = new ArrayList<>();
      terms.addAll(builder.terms);
    }

    private TermBuilderState(Path root, StateMeta meta) {
      this.root = root;
      this.meta = meta;
    }

    public static BuilderState loadFrom(Path from) throws IOException {
      return BuilderState.loadFrom(from, TermBuilderState.class, LOG);
    }

    private List<Term> terms() {
      if (terms != null) {
        return terms;
      }

      if (meta == null || root == null) {
        throw new IllegalStateException("Either terms list or meta file must be non null!");
      }

      Path termsFile = root.resolve(meta.get(TERMS_FILE_PROP)).toAbsolutePath();
      terms = new ArrayList<>();
      try (InputStream is = Files.newInputStream(termsFile)) {
        Term t;
        while ((t = Term.parseDelimitedFrom(is)) != null) {
          terms.add(t);
        }
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Failed to read terms from file [ %s ]", termsFile.toString()), e);
      }

      return terms;
    }

    @Override
    public void saveTo(Path to) throws IOException {
      if (Files.exists(to)) {
        throw new IOException(String.format("Path [ %s ] already exists!", to.toString()));
      }

      Files.createDirectories(to);
      String mappingFileName = "terms.pbl";
      meta = StateMeta.builder(TermBuilderState.class)
          .addProperty(TERMS_FILE_PROP, mappingFileName)
          .build();

      meta.writeTo(to.resolve(META_FILE));
      try (OutputStream os = Files.newOutputStream(to.resolve(mappingFileName))) {
        for (Term t : terms) {
          t.writeDelimitedTo(os);
        }
      }

      root = to;
      terms = null;
    }
  }
}
