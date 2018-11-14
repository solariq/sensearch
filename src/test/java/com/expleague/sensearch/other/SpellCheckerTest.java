package com.expleague.sensearch.other;

import com.expleague.sensearch.core.SpellChecker;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SpellCheckerTest {

  private SpellChecker checker;

  @Before
  public void setUp() throws Exception {
    checker = new SpellChecker(
        Paths.get("/home/mpikalov/Downloads/JamSpell/build/main/jamspell"),
        Paths.get("/home/mpikalov/Downloads/JamSpell/mini_wiki.bin")
    );
  }

  @Test
  public void testSpellChecker() {
    Assert.assertEquals("Привет", checker.correct("Привет"));
    Assert.assertEquals("Будапешт", checker.correct("Будашпет"));
    Assert.assertEquals("Абракадабра", checker.correct("Абракадабра"));
    Assert.assertEquals("Лол кек", checker.correct("Лол \nкек"));
  }
}
