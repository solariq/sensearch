package com.expleague.sensearch.core;

public enum PartOfSpeech {
  A("прилагательное"),
  ADV("наречие"),
  ADVPRO("местоименное наречие"),
  ANUM("числительное-прилагательное"),
  APRO("местоимение-прилагательное"),
  COM("часть композита - сложного слова"),
  CONJ("союз"),
  INTJ("междометие"),
  NUM("числительное"),
  PART("частица"),
  PR("предлог"),
  S("существительное"),
  SPRO("местоимение-существительное"),
  V("глагол");

  final String name;

  PartOfSpeech(String name) {
    this.name = name;
  }
}