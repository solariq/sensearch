package com.expleague.sensearch.cli.commands;

import static com.expleague.sensearch.cli.utils.CommandLineTools.checkOptions;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.EnumOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.plain.JmllEmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.experiments.joom.CrawlerJoom;
import com.expleague.sensearch.experiments.wiki.CrawlerWiki;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class TrainEmbeddingCmd implements Command {

  private static final String COMMAND_NAME = "train-emb";

  private static final PathOption DATA_PATH = PathOption.builder()
      .shortOption("d")
      .longOption("data")
      .description("Specify the path to a corpus")
      .predicates(ExistingPath.get())
      .build();
  private static final EnumOption<DataCrawler> DATA_CRAWLER = EnumOption.builder(DataCrawler.class)
      .shortOption("c")
      .longOption("data-crawler")
      .description("'WIKI' or 'JOOM'")
      .build();
  private static final IntOption VECTORS_LENGTH = IntOption.builder()
      .shortOption("l")
      .longOption("vec-length")
      .defaultValue(PlainIndexBuilder.DEFAULT_VEC_SIZE)
      .description("Specify the length of word embeddings")
      .build();
  private static final PathOption TMP_OUTPUT_PATH = PathOption.builder()
      .longOption("tmp-output")
      .defaultValue("Specify temporary output for embedding")
      .predicates(ExistingPath.negated())
      .build();
  private static final PathOption OUTPUT_PATH = PathOption.builder()
      .shortOption("o")
      .longOption("output")
      .description("Specify the output path for a trained embedding")
      .predicates(ExistingPath.negated())
      .build();

  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .numberOfArgs(0)
      .required(false)
      .build();

  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  private static final TrainEmbeddingCmd INSTANCE = new TrainEmbeddingCmd();

  static {
    DATA_PATH.addToOptions(OPTIONS);
    DATA_CRAWLER.addToOptions(OPTIONS);
    VECTORS_LENGTH.addToOptions(OPTIONS);
    TMP_OUTPUT_PATH.addToOptions(OPTIONS);
    OUTPUT_PATH.addToOptions(OPTIONS);

    OPTIONS.addOption(HELP);
  }

  private TrainEmbeddingCmd() {
  }

  public static TrainEmbeddingCmd instance() {
    return INSTANCE;
  }

  public static String commandName() {
    return COMMAND_NAME;
  }

  @Override
  public void run(String... args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (Exception e) {
      printHelp();
      return;
    }

    if (commandLine.hasOption(HELP.getOpt())) {
      printHelp();
      return;
    }

    checkOptions(commandLine, DATA_PATH, DATA_CRAWLER, VECTORS_LENGTH,
        TMP_OUTPUT_PATH, OUTPUT_PATH);

    try (Writer w = Files.newBufferedWriter(OUTPUT_PATH.value(commandLine))) {
      final JmllEmbeddingBuilder embeddingBuilder =
          new JmllEmbeddingBuilder(VECTORS_LENGTH.value(commandLine),
              TMP_OUTPUT_PATH.value(commandLine));
      Crawler crawler = DATA_CRAWLER.value(commandLine)
          .crawlerClass()
          .getConstructor(Path.class)
          .newInstance(DATA_PATH.value(commandLine));
      EmbeddingImpl<CharSeq> embedding = (EmbeddingImpl<CharSeq>) embeddingBuilder
          .build(crawler.makeStream());
      embedding.write(w);
    }
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }

  private enum DataCrawler {
    JOOM(CrawlerJoom.class),
    WIKI(CrawlerWiki.class);

    private final Class<? extends Crawler> crawlerClass;

    DataCrawler(Class<? extends Crawler> crawlerClass) {
      this.crawlerClass = crawlerClass;
    }

    Class<? extends Crawler> crawlerClass() {
      return crawlerClass;
    }
  }
}
