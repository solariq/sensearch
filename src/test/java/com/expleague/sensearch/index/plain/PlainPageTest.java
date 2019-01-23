package com.expleague.sensearch.index.plain;

import static org.junit.Assert.assertEquals;

import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.utils.IndexBasedTestCase;
import java.net.URI;
import org.junit.Test;

public class PlainPageTest extends IndexBasedTestCase {

  private static final URI TEST_PAGE_URI = URI
      .create("https://ru.wikipedia.org/Тестовый_заголовок");
  private final IndexedPage rootPage = (IndexedPage) index().page(TEST_PAGE_URI);

  @Test
  public void parentId() {
    assertEquals(rootPage.parentId(), rootPage.id());
  }

  @Test
  public void subpagesIds() {
  }

  @Test
  public void testUri() {
    assertEquals(TEST_PAGE_URI, rootPage.uri());
  }

  @Test
  public void content() {
  }

  @Test
  public void categories() {
  }

  @Test
  public void outgoingLinks() {
  }

  @Test
  public void incomingLinks() {
  }

  @Test
  public void parent() {
  }

  @Test
  public void subpages() {
  }

  @Test
  public void sentences() {
  }
}