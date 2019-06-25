package com.expleague.sensearch.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.core.Tokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class TokenizerImplTest {

  private static final Pattern SENTENCE_SPLIT_PATTEN =
      Pattern.compile("(?<=[.!?]|[.!?]['\"])(?=\\p{javaWhitespace}*\\p{javaUpperCase})");
  private static final Pattern WORD_SPLIT_PATTERN =
      Pattern.compile("[^\\p{javaAlphabetic}\\p{javaDigit}]");

  @Test
  public void testToSentences() {
    TokenizerImpl tokenizer = new TokenizerImpl();
    String text =
        " Some text... В нем есть русский! Ё-маЁ?! \"Что-то в кавычках...\" кек. "
            + "Кек. И Он сказал: 'хрень.' Ч. т. д. 'Мда'... The end ";

    List<CharSequence> sentences = tokenizer.toSentences(text).collect(Collectors.toList());

    assertEquals("Some text...", sentences.get(0));
    assertEquals("В нем есть русский!", sentences.get(1));
    assertEquals("Ё-маЁ?!", sentences.get(2));
    assertEquals("\"Что-то в кавычках...\" кек.", sentences.get(3));
    assertEquals("Кек.", sentences.get(4));
    assertEquals("И Он сказал: 'хрень.'", sentences.get(5));
    assertEquals("Ч. т. д.", sentences.get(6));
    assertEquals("'Мда'...", sentences.get(7));
    assertEquals("The end", sentences.get(8));
  }

  @Test
  public void testToWordsStrange() {
    TokenizerImpl tokenizer = new TokenizerImpl();
    String text = "!@#$%№^&*()_+=-1234567890 \n \t qwer-tyuiop[]asd1fghjkl;'zxcv0bnm,./";
    List<CharSeq> expected = new ArrayList<>();
    expected.add(CharSeq.create("!"));
    expected.add(CharSeq.create("@"));
    expected.add(CharSeq.create("#"));
    expected.add(CharSeq.create("$"));
    expected.add(CharSeq.create("%"));
    expected.add(CharSeq.create("№"));
    expected.add(CharSeq.create("^"));
    expected.add(CharSeq.create("&"));
    expected.add(CharSeq.create("*"));
    expected.add(CharSeq.create("("));
    expected.add(CharSeq.create(")"));
    expected.add(CharSeq.create("_"));
    expected.add(CharSeq.create("+"));
    expected.add(CharSeq.create("="));
    expected.add(CharSeq.create("-"));
    expected.add(CharSeq.create("1234567890"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("\n"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("\t"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("qwer"));
    expected.add(CharSeq.create("-"));
    expected.add(CharSeq.create("tyuiop"));
    expected.add(CharSeq.create("["));
    expected.add(CharSeq.create("]"));
    expected.add(CharSeq.create("asd1fghjkl"));
    expected.add(CharSeq.create(";"));
    expected.add(CharSeq.create("'"));
    expected.add(CharSeq.create("zxcv0bnm"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create("."));
    expected.add(CharSeq.create("/"));
    List<CharSequence> ans = tokenizer.toWords(CharSeq.create(text)).collect(Collectors.toList());
    Assert.assertEquals(expected, ans);
  }

  @Test
  public void testToWords() {
    Tokenizer tokenizer = new TokenizerImpl();
    String text = "\"heLLo, privEt!23xyi,_ %@()\\\".;@ kl\t \\n wo'w\"";
    List<CharSeq> expected = new ArrayList<>();
    expected.add(CharSeq.create("\""));
    expected.add(CharSeq.create("heLLo"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("privEt"));
    expected.add(CharSeq.create("!"));
    expected.add(CharSeq.create("23xyi"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create("_"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("%"));
    expected.add(CharSeq.create("@"));
    expected.add(CharSeq.create("("));
    expected.add(CharSeq.create(")"));
    expected.add(CharSeq.create("\\"));
    expected.add(CharSeq.create("\""));
    expected.add(CharSeq.create("."));
    expected.add(CharSeq.create(";"));
    expected.add(CharSeq.create("@"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("kl"));
    expected.add(CharSeq.create("\t"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("\\"));
    expected.add(CharSeq.create("n"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("wo"));
    expected.add(CharSeq.create("'"));
    expected.add(CharSeq.create("w"));
    expected.add(CharSeq.create("\""));
    List<CharSequence> ans = tokenizer.toWords(CharSeq.create(text)).collect(Collectors.toList());
    Assert.assertEquals(expected, ans);
  }

  @Test
  public void testToSentencesPerformance() {
    TokenizerImpl tokenizer = new TokenizerImpl();

    String text =
        "Британская национальная антарктическая экспедиция 1901—1904 годов (англ. British National Antarctic Expedition, 1901–04), также известная как экспеди́ция на «Дискавери» (англ. Discovery Expedition) — вторая по счёту британская экспедиция в Антарктиду, организованная после более чем 60-летнего перерыва. Её целью было комплексное исследование тогда почти совершенно неизвестного континента Земли. Экспедиционное судно — барк «Дискавери» — было первым из специально построенных для британских научно-исследовательских миссий кораблей. В ходе экспедиции был обследован берег Антарктиды в море Росса от мыса Адэр до Китовой бухты. Британской командой были получены обширные научные данные в области физической географии, биологии, геологии, метеорологии, земного магнетизма. Были открыты антарктические оазисы, а также колонии пингвинов на мысе Крозье[en]. В ходе санного похода была достигнута точка 82°11′ ю. ш.\n"
            + "\n"
            + "Главным организатором и пропагандистом экспедиции стал президент Королевского географического общества сэр Клементс Маркэм, который занимался этой работой с 1893 года и добился того, что 50 % её бюджета взяло на себя государство. Начальником экспедиции по его протекции был назначен военный моряк Роберт Скотт. В команду вошли ставшие в будущем известными исследователи Антарктики, такие как Эрнест Генри Шеклтон, Эдвард Адриан Уилсон и Фрэнк Уайлд. Единственными участниками экспедиции, имевшими полярный опыт, были второй помощник Альберт Армитедж и врач Реджинальд Кётлиц, участвовавшие в исследовании Земли Франца-Иосифа. Однако им не удалось найти общего языка с Робертом Скоттом и передать ему свой опыт.";
    for (int i = 0; i < 100000; i++) {
      tokenizer.toSentences(text);
      toSentencesRegexp(text);
    }

    long startTime = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
      tokenizer.toSentences(text);
    }
    System.out.println("Test to sentences performance");
    double tokenizerTime = (System.nanoTime() - startTime) / 1e9;
    System.out.println("Tokenizer time: " + tokenizerTime);

    startTime = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
      toSentencesRegexp(text);
    }
    double regexTime = (System.nanoTime() - startTime) / 1e9;
    System.out.println("Regex time: " + regexTime);

    assertTrue(tokenizerTime < regexTime);
  }

  @Test
  public void testToWordsPerformance() {
    TokenizerImpl tokenizer = new TokenizerImpl();

    String text =
        "Британская национальная антарктическая экспедиция 1901—1904 годов (англ. British National Antarctic Expedition, 1901–04), также известная как экспеди́ция на «Дискавери» (англ. Discovery Expedition) — вторая по счёту британская экспедиция в Антарктиду, организованная после более чем 60-летнего перерыва. Её целью было комплексное исследование тогда почти совершенно неизвестного континента Земли. Экспедиционное судно — барк «Дискавери» — было первым из специально построенных для британских научно-исследовательских миссий кораблей. В ходе экспедиции был обследован берег Антарктиды в море Росса от мыса Адэр до Китовой бухты. Британской командой были получены обширные научные данные в области физической географии, биологии, геологии, метеорологии, земного магнетизма. Были открыты антарктические оазисы, а также колонии пингвинов на мысе Крозье[en]. В ходе санного похода была достигнута точка 82°11′ ю. ш.\n"
            + "\n"
            + "Главным организатором и пропагандистом экспедиции стал президент Королевского географического общества сэр Клементс Маркэм, который занимался этой работой с 1893 года и добился того, что 50 % её бюджета взяло на себя государство. Начальником экспедиции по его протекции был назначен военный моряк Роберт Скотт. В команду вошли ставшие в будущем известными исследователи Антарктики, такие как Эрнест Генри Шеклтон, Эдвард Адриан Уилсон и Фрэнк Уайлд. Единственными участниками экспедиции, имевшими полярный опыт, были второй помощник Альберт Армитедж и врач Реджинальд Кётлиц, участвовавшие в исследовании Земли Франца-Иосифа. Однако им не удалось найти общего языка с Робертом Скоттом и передать ему свой опыт.";
    for (int i = 0; i < 100000; i++) {
      tokenizer.toWords(text);
      parseToWordsRegexp(text);
    }

    long startTime = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
      tokenizer.toWords(text);
    }
    double tokenizerTime = (System.nanoTime() - startTime) / 1e9;
    System.out.println("Test to words performance");
    System.out.println("Tokenizer time: " + tokenizerTime);

    startTime = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
      parseToWordsRegexp(text);
    }
    double regexTime = (System.nanoTime() - startTime) / 1e9;
    System.out.println("Regex time: " + regexTime);

    assertTrue(tokenizerTime < regexTime);
  }

  private List<CharSequence> parseToWordsRegexp(CharSequence sentence) {
    return Stream.<CharSequence>of(
        WORD_SPLIT_PATTERN.split(
            CharSeqTools.replace(sentence, String.valueOf((char) 769), "")))
        .map(CharSeqTools::trim)
        .filter(s -> s.length() > 0)
        .collect(Collectors.toList());
  }

  private List<CharSequence> toSentencesRegexp(CharSequence text) {
    return Stream.<CharSequence>of(SENTENCE_SPLIT_PATTEN.split(text))
        .map(CharSeqTools::trim)
        .filter(s -> s.length() > 0)
        .collect(Collectors.toList());
  }
}
