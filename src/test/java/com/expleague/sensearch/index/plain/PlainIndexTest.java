package com.expleague.sensearch.index.plain;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.utils.IndexBasedTestCase;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class PlainIndexTest extends IndexBasedTestCase {
  @Test
  public void testFetchDocuments() {
    // TODO: what do we test there?
  }

  @Test
  public void testTerm_emptyWord() {
    Term term = index().term("");
    assertNull(term);
  }

  @Test
  public void testTerm_missing() {
    // This word is not in the test MiniWiki
    Term term = index().term("китаец");
    assertNull(term);
  }

  @Test
  public void testTerm_exists() {
    // Crawler xml with id 3774964
    String word = "ведущий";

    Term term = index().term(word);
    assertNotNull(term);
    assertEquals("ведущий", term.text().toString());

    assertEquals(CharSeq.create("ведущ"), term.lemma().text());
    // This term occurs at least once
    assertTrue(term.documentFreq() > 0);
    assertTrue(term.freq() > 0);

    // TODO: currently pos are disabled
//    assertEquals(PartOfSpeech.A, term.partOfSpeech());
  }

  @Test
  public void testTerm_existsCase() {
    // Crawler xml with id 3774964
    String word = "ВедуЩиЙ";

    Term term = index().term(word);
    assertNotNull(term);
    assertEquals("ведущий", term.text().toString());
    // Lemma of "ведущий" is "ведущий"
    assertEquals(CharSeq.create("ведущ"), term.lemma().text());
    // This term occurs at least once
    assertTrue(term.documentFreq() > 0);
    assertTrue(term.freq() > 0);
    // TODO
//    assertEquals(PartOfSpeech.A, term.partOfSpeech());
  }

  @Test
  public void testTerm_lemmaFreq() {
    String word = "смерти";
    Term term = index().term(word);
    assertEquals(term.documentLemmaFreq(), term.lemma().documentLemmaFreq());

    word = "смерт";
    term = index().term(word);
    assertEquals(term.documentLemmaFreq(), term.lemma().documentLemmaFreq());

    word = "ведущий";
    term = index().term(word);
    assertEquals(term.documentLemmaFreq(), term.lemma().documentLemmaFreq());

  }

  @Test
  public void testTerm_lemma() {
    // Crawler xml with id 20558
    String word = "смерти";
    Term term = index().term(word);

    String lemma = "смерт";
    Term lemmaTerm = index().term(lemma);

    assertNotNull(term);
    assertNotNull(lemmaTerm);

    assertEquals("смерти", term.text().toString());

    assertSame(lemmaTerm, term.lemma());
    assertSame(lemmaTerm, lemmaTerm.lemma());

    assertEquals(PartOfSpeech.S, term.partOfSpeech());
    assertEquals(PartOfSpeech.S, lemmaTerm.partOfSpeech());
  }

  @Test
  public void testSentences_oneSentence() {
    String text = "Ведущий смерти.";
    List<CharSequence> sentences = index().sentences(text).collect(Collectors.toList());

    assertEquals(1, sentences.size());
    assertEquals("Ведущий смерти.", sentences.get(0));
  }

  @Test
  public void testSentences_multipleSentences() {
    String text =
        "Неограниченная власть не означает, что диктатор единолично принимает все принципиальные решения, поскольку это физически невозможно! Также следует отметить, что при нём могут быть (и часто бывают) совещательные органы, формально наделённые высшими властными полномочиями... Однако в условиях диктатуры любые такие органы издают свои постановления и указы согласно воле диктатора";

    List<CharSequence> sentences = index().sentences(text).collect(Collectors.toList());

    assertEquals(3, sentences.size());
    assertEquals(
        "Неограниченная власть не означает, что диктатор единолично принимает все принципиальные решения, поскольку это физически невозможно!",
        sentences.get(0));
    assertEquals(
        "Также следует отметить, что при нём могут быть (и часто бывают) совещательные органы, формально наделённые высшими властными полномочиями...",
        sentences.get(1));
    assertEquals(
        "Однако в условиях диктатуры любые такие органы издают свои постановления и указы согласно воле диктатора",
        sentences.get(2));
  }

  @Test
  public void test_parse() {
    String text = "Ведущий смерти. И неограниченная власть...";

    List<Term> terms = index().parse(text).collect(Collectors.toList());

    assertEquals(5, terms.size());

    assertSame(index().term("ведущий"), terms.get(0));
    assertSame(index().term("смерти"), terms.get(1));
    assertSame(index().term("и"), terms.get(2));
    assertSame(index().term("неограниченная"), terms.get(3));
    assertSame(index().term("власть"), terms.get(4));
  }

  @Test
  public void testPage_missing() {
    URI uri = URI.create("https://ru.wikipedia.org/ПолнаяХрень");

    assertSame(PlainPage.EMPTY_PAGE, index().page(uri));
  }

  @Test
  public void testPage_existing() {
    URI uri = URI.create("https://ru.wikipedia.org/wiki/Диктатор");
    Page page = index().page(uri);

    assertNotSame(PlainPage.EMPTY_PAGE, page);
  }

//  @Ignore("Sections are not indexed for now")
  @Test
  public void testPage_existingSection() {
    URI uri = URI.create("https://ru.wikipedia.org/wiki/Диктатор#Римские_диктаторы");

    Page page = index().page(uri);
    assertNotSame(PlainPage.EMPTY_PAGE, page);
  }

//  @Ignore("Sections are not indexed for now")
  @Test
  public void testPage_sectionContent() {
    String content =
        "Журнал Foreign Policy в 2010 году опубликовал список 23 худших диктаторов современности. По состоянию на ноябрь 2017 года 12 правителей из списка лишились власти, из них 5 умерли.";
    URI uri =
        URI.create(
            "https://ru.wikipedia.org/wiki/Диктатор#Диктаторы_XXI_века_по_версии_журнала_The_Foreign_Policy");
    Page page = index().page(uri);
    assertEquals(content, page.content(SegmentType.BODY));
  }

  @Test
  public void testPage_content() {
    String content =
        "Суффет — название двух главных должностных лиц ( магистратов ) в Тире , а также в северной Африке, на территории Карфагенской республики . Обычно они были верховными судьями. Во время военных действий часто — главнокомандующими. В древнем Израиле так называли военных предводителей и судей.";

    String fullContent =
        content
            + "\n«судья», , родительный падеж sufetis.\n"
            + "Суффеты существовали с V век до н. э. Они избирались ежегодно на народных собраниях.\n"
            + "Эпоха Судей \n"
            + ", Исследования и публикации по истории античного мира. Под редакцией профессора Э. Д. Фролова. Выпуск 3. Санкт-Петербург, 2004.";

    URI uri = URI.create("https://ru.wikipedia.org/wiki/Суффет");
    Page page = index().page(uri);

    assertEquals(content, page.content(SegmentType.SUB_BODY));
    assertEquals(fullContent, page.content(SegmentType.BODY));
  }

  @Test
  public void testSize() {
    assertEquals(32, index().size());
  }

  @Test
  public void testVocabularySize() {
    assertTrue(index().vocabularySize() > 0);
  }

  @Test
  public void testAveragePageSize() {
    assertTrue(index().averagePageSize() > 0);
  }
}
