package com.expleague.sensearch.donkey.builders;

import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory;

public class PageStatisticsBuilderFactory {

  private final SerializedTextHelperFactory helperFactory;

  public PageStatisticsBuilderFactory(SerializedTextHelperFactory helperFactory) {
    this.helperFactory = helperFactory;
  }

  public PageStatisticsBuilder builder(long pageId) {
    return new PageStatisticsBuilder(helperFactory, pageId);
  }

}