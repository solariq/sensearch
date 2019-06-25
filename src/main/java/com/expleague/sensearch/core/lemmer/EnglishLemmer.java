package com.expleague.sensearch.core.lemmer;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.commons.text.lemmer.WordInfo;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class EnglishLemmer implements Lemmer {
  private WordnetStemmer wordnetStemmer;

  public EnglishLemmer() {
    String path = "./resources/dict";
    if (!Files.exists(Paths.get(path))) {
      throw new IllegalArgumentException(
          "Missing english disctionary. Please download it from gDrive");
    }

    URL url = null;
    try {
      url = new URL("file", null, path);
    } catch (MalformedURLException ignored) {
    }

    if (url == null) return;

    IDictionary dictionary = new Dictionary(url);
    try {
      dictionary.open();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    this.wordnetStemmer = new WordnetStemmer(dictionary);
  }

  private PartOfSpeech convert(POS pos) {
    switch (pos) {
      case NOUN:
        return PartOfSpeech.S;
      case VERB:
        return PartOfSpeech.V;
      case ADJECTIVE:
        return PartOfSpeech.A;
      case ADVERB:
      default:
        return PartOfSpeech.ADV;
    }
  }

  public WordInfo parse(CharSequence cs) {

    String word = cs.toString();
    String lemma = null;
    POS partOfSpeech = null;

    for (POS pos : POS.values()) {
      List<String> lexemes = wordnetStemmer.findStems(word, pos);
      if (!lexemes.isEmpty()) {
        lemma = lexemes.get(0);
        partOfSpeech = pos;
        break;
      }
    }

    if (lemma == null) {
      lemma = word;
    }

    if (partOfSpeech == null) {
      partOfSpeech = POS.ADVERB;
    }

    return new WordInfo(
        CharSeq.create(word),
        Collections.singletonList(new LemmaInfo(CharSeq.create(lemma), 1, convert(partOfSpeech))));
  }
}
