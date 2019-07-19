package com.expleague.sensearch.index;

public interface IndexCharacteristics extends TermStatisticsBase {
  long version();
  int embeddingVectorLength();

  int documentsCount();
  int titlesCount();
  
  long titleTermsCount();
  long contentTermsCount();
  long linkTermsCount();

  int incomingLinksCount();
  int outgoingLinksCount();
  int targetTitleTermsCount();
}
