package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by sandulmv on 11.11.18.
 */
public class EmbeddingBuilder {
  private static final Logger LOG = Logger.getLogger(EmbeddingBuilder.class.getName());

  private final Config config;
  private final TObjectIntMap<String> wordIdMappings;
  EmbeddingBuilder(Config config, TObjectIntMap<String> wordIdMappings) {
    this.wordIdMappings = wordIdMappings;
    this.config = config;;

  }

  void addNewPageVector(CrawlerDocument crawlerDocument, long pageId) {
  }
}
