package com.expleague.sensearch.index.plain;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.utils.IndexBasedTestCase;
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
  public void testTerm_exists() {
    // Crawler xml with id 6673315
    String word = "китаец";

    Term term = index().term(word);
    assertNotNull(term);
    assertEquals("китаец", term.text().toString());
    // Lemma of "китаец" is "китаец"
    assertSame(term, term.lemma());
    // This term occurs at least once
    assertTrue(term.documentFreq() > 0);
    assertTrue(term.freq() > 0);
    assertEquals(PartOfSpeech.S, term.partOfSpeech());
  }
}
