package com.expleague.sensearch;

import com.expleague.sensearch.core.Term;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.jetbrains.annotations.Nullable;

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

  Stream<Page> subpages();

  Stream<CharSequence> sentences(SegmentType type);
  Stream<Term> parse(CharSequence sequence);

  interface Link {

    CharSequence content();

    CharSequence text();

    boolean hasTarget();

    Page targetPage();

    Page sourcePage();

    long position();
  }
}
