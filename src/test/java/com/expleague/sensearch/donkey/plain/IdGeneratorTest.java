package com.expleague.sensearch.donkey.plain;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;


public class IdGeneratorTest {

  private IdGenerator idGenerator = new IdGenerator();

  @Test
  public void testTermIds() {
    assertTrue(idGenerator.termId("some term") > 0);
    Set<Long> ids = new HashSet<>();

    for (int i = 0; i < 100; i++) {
      long id = idGenerator.termId(Integer.toString(i));
      assertTrue(id > 0);
      ids.add(id);
    }

    assertEquals(100, ids.size());
  }

  @Test
  public void testUriIds() {
    assertTrue(idGenerator.pageId(URI.create("someUri")) < 0);
    assertTrue(idGenerator.sectionId(URI.create("someUri")) < 0);

    Set<Long> ids = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      long pageId = idGenerator.pageId(URI.create(Integer.toString(i)));
      assertTrue(pageId < 0);
      long sectionId = idGenerator.sectionId(URI.create(Integer.toString(i)));
      assertTrue(sectionId < 0);

      // May be changed in the future
      assertEquals(pageId, sectionId);
      ids.add(pageId);
    }

    assertEquals(100, ids.size());
  }
}
