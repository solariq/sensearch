package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import java.net.URI;

public class PlainPage implements IndexedPage {

  private final long id;
  private final String text;
  private final String title;

  /**
   * Empty Page constructor
   */
  PlainPage() {
    id = 0;
    text = "";
    title = "";
  }

  PlainPage(IndexUnits.Page page) {
    text = page.getContent();
    title = page.getTitle();
    id = page.getPageId();
  }

  @Override
  public long id() {
    return this.id;
  }

  @Override
  public URI reference() {
    return URI.create("http://ru.wikipedia.org/wiki/" + title().toString().replace(" ", "_"));
  }

  @Override
  public CharSequence text() {
    return text;
  }

  @Override
  public CharSequence title() {
    return title;
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof PlainPage) && (this == other || ((PlainPage) other).id == this.id);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.id);
  }
}
