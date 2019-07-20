package com.expleague.sensearch.donkey.statistics;

import com.expleague.sensearch.index.IndexCharacteristics;
import java.nio.file.Path;

public class PlainIndexCharacteristics implements IndexCharacteristics {


  public static IndexCharacteristics readFrom(Path from) {
    return null;
  }

  @Override
  public long version() {
    return 0;
  }

  @Override
  public int embeddingVectorLength() {
    return 0;
  }

  @Override
  public int documentsCount() {
    return 0;
  }

  @Override
  public int titlesCount() {
    return 0;
  }

  @Override
  public long titleTermsCount() {
    return 0;
  }

  @Override
  public long contentTermsCount() {
    return 0;
  }

  @Override
  public long linkTermsCount() {
    return 0;
  }

  @Override
  public int linksCount() {
    return 0;
  }

  @Override
  public int targetTitleTermsCount() {
    return 0;
  }

  @Override
  public void saveTo(Path to) {

  }
}
