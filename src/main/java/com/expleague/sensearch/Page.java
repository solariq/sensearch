package com.expleague.sensearch;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Page {

  @NotNull
  URI uri();

  @NotNull
  CharSequence title();

  @NotNull
  CharSequence fullContent();

  @NotNull
  CharSequence content();

  @Deprecated
  List<CharSequence> categories();

  @NotNull
  Stream<Link> outcomingLinks();

  @NotNull
  Stream<Link> incomingLinks();

  @NotNull
  Page parent();

  Stream<Page> subpages();

  interface Link {

    CharSequence text();

    boolean hasTarget();

    Page targetPage();

    Page sourcePage();

    long position();
  }
}
