package com.expleague.sensearch.experiments.joom;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class JoomPage implements CrawlerDocument {

  private final static String JOOM_ROOT = "https://www.joom.com/en/products/";
  private final String title;
  private final Section description;
  private final URI uri;

  public JoomPage(String title, String description, String id) {
    this.uri = URI.create(JOOM_ROOT + id);
    this.title = title;
    this.description = new Section() {
      @Override
      public CharSequence text() {
        return description;
      }

      @Override
      public URI uri() {
        return URI.create(JOOM_ROOT + id);
      }

      @Override
      public List<CharSequence> titles() {
        return Collections.singletonList(title);
      }

      @Override
      public CharSequence title() {
        return title;
      }

      @Override
      public List<Link> links() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public CharSequence content() {
    return description.text();
  }

  @Override
  public String title() {
    return title;
  }

  @Override
  public List<String> categories() {
    return Collections.emptyList();
  }

  @Override
  public Stream<Section> sections() {
    return Stream.of(description);
  }

  @Override
  public URI uri() {
    return uri;
  }
}
