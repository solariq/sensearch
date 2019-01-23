package com.expleague.sensearch;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

public interface Page {

  enum SegmentType {
    TITLE, //All titles
    BODY,  //Full body
    SUB_TITLE,
    SUB_BODY
  }

  @NotNull
  URI uri();

  @NotNull
  CharSequence content(SegmentType... types);

  @NotNull
  List<CharSequence> categories();

  @NotNull
  Stream<Link> outgoingLinks();

  @NotNull
  Stream<Link> incomingLinks();

  @NotNull
  Page parent();

  boolean hasParent();

  Stream<Page> subpages();

  Stream<CharSequence> sentences(SegmentType type);

  interface Link {

    CharSequence content();

    CharSequence text();

    boolean hasTarget();

    Page targetPage();

    Page sourcePage();

    long position();
  }
}
