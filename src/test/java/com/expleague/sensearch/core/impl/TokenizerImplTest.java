package com.expleague.sensearch.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.expleague.commons.seq.CharSeqTools;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  public void testToWords() {
    TokenizerImpl tokenizer = new TokenizerImpl();
    String text =
        " Some text... В нем есть русский! Ё-маЁ?! \"Что-то в кавычках...\" кек. "
            + "Кек. И Он сказал: 'хрень.' Ч. т. д. 'Мда'... The end ";
    assertEquals(tokenizer.toWords(text).collect(Collectors.toList()), parseToWordsRegexp(text));
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
