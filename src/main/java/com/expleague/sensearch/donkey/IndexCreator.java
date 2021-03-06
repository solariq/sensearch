package com.expleague.sensearch.donkey;

public interface IndexCreator {

  void createWordEmbedding();

  void createPagesAndTerms();

  void createLinks();

  void createStats();

  void createPageEmbedding();

  void createSuggest();
}
