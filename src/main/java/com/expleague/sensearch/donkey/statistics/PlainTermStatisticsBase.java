package com.expleague.sensearch.donkey.builders;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.randomaccess.RandomAccess;
import com.expleague.sensearch.index.TermStatisticsBase;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.iq80.leveldb.DB;

public class PlainTermStatisticsBase implements TermStatisticsBase {

  public PlainTermStatisticsBase(RandomAccess<Integer, Term> termBase) {

  }


  public static TermStatistics readFrom(Path from) {
    return null;
  }

  @Override
  public long termFrequency(Term term) {
    return 0;
  }

  @Override
  public int documentFrequency(Term term) {
    return 0;
  }

  @Override
  public Stream<Pair<Term, Integer>> mostFrequentNeighbours(Term term, int neighCount) {
    return null;
  }

  @Override
  public void saveTo(Path path) {

  }
}
