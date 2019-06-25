package com.expleague.sensearch.donkey.utils;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.core.lemmer.Lemmer;
import com.expleague.sensearch.donkey.utils.TokenParser.Token;
import com.expleague.sensearch.donkey.writers.Writer;
import java.util.Collections;
import org.junit.Test;

public class TokenParserTest {

  private static final Writer<ParsedTerm> dummyWriter = new Writer<ParsedTerm>() {
    @Override
    public void write(ParsedTerm object) {
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
    }
  };

  private static final Lemmer dummyLemmer = seq -> {
    switch (seq.toString()) {
      case "cats":
        return new WordInfo(CharSeq.create("cats"),
            Collections.singletonList(new LemmaInfo(CharSeq.create("cat"), 1, PartOfSpeech.S)));
      case "catsss":
        return new WordInfo(CharSeq.create("catsss"),
            Collections.singletonList(new LemmaInfo(CharSeq.create("cat"), 1, PartOfSpeech.S)));
      case "cat":
        return new WordInfo(CharSeq.create("cat"),
            Collections.singletonList(new LemmaInfo(CharSeq.create("cat"), 1, PartOfSpeech.S)));
      default:
        return new WordInfo(CharSeq.create(seq), Collections.emptyList());
    }
  };

  @Test
  public void testAddToken() {
    TokenParser tokenParser = new TokenParser(new Dictionary(dummyWriter), dummyLemmer, new TokenizerImpl());
    Token cats = tokenParser.addToken("Cats");
    Token cat = tokenParser.addToken("CAT");
    Token trash = tokenParser.addToken("trash");
    Token catsss = tokenParser.addToken(CharSeq.create("catsss"));
    Token comma = tokenParser.addToken(CharSeq.create(","));
    Token space = tokenParser.addToken(" ");

    assertEquals("cats", cats.text().toString());
    assertTrue(cats.firstUpperCase());
    assertTrue(TokenParser.firstUpperCase(cats.formId()));

    assertFalse(cats.allUpperCase());
    assertFalse(TokenParser.allUpperCase(cats.formId()));

    assertTrue(cats.isWord());
    assertTrue(TokenParser.isWord(cats.formId()));
    assertFalse(TokenParser.isPunct(cats.formId()));

    assertEquals("cat", cat.text().toString());

    assertTrue(cat.firstUpperCase());
    assertTrue(TokenParser.firstUpperCase(cat.formId()));

    assertTrue(cat.allUpperCase());
    assertTrue(TokenParser.allUpperCase(cat.formId()));

    assertTrue(cat.isWord());
    assertTrue(TokenParser.isWord(cat.formId()));
    assertFalse(TokenParser.isPunct(cat.formId()));

    assertEquals("trash", trash.text().toString());

    assertFalse(trash.firstUpperCase());
    assertFalse(TokenParser.firstUpperCase(trash.formId()));

    assertFalse(TokenParser.allUpperCase(trash.formId()));
    assertFalse(trash.allUpperCase());

    assertTrue(trash.isWord());
    assertTrue(TokenParser.isWord(trash.formId()));
    assertFalse(TokenParser.isPunct(trash.formId()));

    assertEquals("catsss", catsss.text().toString());

    assertFalse(catsss.firstUpperCase());
    assertFalse(TokenParser.firstUpperCase(catsss.formId()));

    assertFalse(catsss.allUpperCase());
    assertFalse(TokenParser.allUpperCase(catsss.formId()));

    assertTrue(catsss.isWord());
    assertTrue(TokenParser.isWord(catsss.formId()));
    assertFalse(TokenParser.isPunct(catsss.formId()));

    assertEquals(",", comma.text().toString());

    assertFalse(comma.firstUpperCase());
    assertFalse(TokenParser.firstUpperCase(comma.formId()));

    assertFalse(comma.allUpperCase());
    assertFalse(TokenParser.allUpperCase(comma.formId()));

    assertFalse(comma.isWord());
    assertFalse(TokenParser.isWord(comma.formId()));
    assertTrue(TokenParser.isPunct(comma.formId()));

    assertEquals(" ", space.text().toString());

    assertFalse(space.firstUpperCase());
    assertFalse(TokenParser.firstUpperCase(space.formId()));

    assertFalse(space.allUpperCase());
    assertFalse(TokenParser.allUpperCase(space.formId()));

    assertFalse(space.isWord());
    assertFalse(TokenParser.isWord(space.formId()));
    assertTrue(TokenParser.isPunct(space.formId()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddToken_empty() {
    TokenParser tokenParser = new TokenParser(new Dictionary(dummyWriter), dummyLemmer, new TokenizerImpl());
    tokenParser.addToken("");
  }
}