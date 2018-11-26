package com.expleague.sensearch.other;

import com.expleague.sensearch.core.SpellChecker;
import org.junit.Assert;
import org.junit.Test;

public class SpellCheckerTest {

  private SpellChecker checker = new SpellChecker();

  @Test
  public void testJlangTool() throws Exception {
    Assert.assertEquals("Красный", checker.correct("Красый"));
    Assert.assertEquals("Проверка", checker.correct("Прверка"));
    Assert.assertEquals("Молоко", checker.correct("Малако"));
    Assert.assertEquals("Прилагательное", checker.correct("Прелагательное"));
    Assert.assertEquals("Красивый", checker.correct("Красивый"));
    Assert.assertEquals("Два слова", checker.correct("Два слова"));
    Assert.assertEquals("", checker.correct(""));
    Assert.assertEquals("Бело-черный", checker.correct("Бела-черый"));
  }
}
