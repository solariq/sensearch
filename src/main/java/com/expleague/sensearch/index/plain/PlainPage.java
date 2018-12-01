package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import java.net.URI;

public class PlainPage implements IndexedPage {

  private final long id;
  private final String text;
  private final String title;
  private final URI uri;

  PlainPage(IndexUnits.Page page) {
    text = page.getContent();
    title = page.getTitle();
    uri = URI.create(page.getUri());
    id = page.getPageId();
  }

  @Override
  public long id() {
    return this.id;
  }

  @Override
  public URI reference() {
    return uri;
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
