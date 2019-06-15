package com.expleague.sensearch;

import com.expleague.commons.math.vectors.Vec;

import com.expleague.sensearch.core.Term;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

public interface Page {

  CharSequence TITLE_DELIMETER = " # ";

  enum SegmentType {
    FULL_TITLE, // All titles
    BODY, // Full body
    SECTION_TITLE,
    SUB_BODY
  }

  /**
   * Which links to return SELF_LINKS includes links to this section, its parents and subsections
   */
  enum LinkType {
    SECTION_LINKS,
    ALL_LINKS,
  }

  @NotNull
  URI uri();

  @NotNull
  Stream<Term> content(SegmentType... types);

  @NotNull
  List<List<Term>> categories();

  @NotNull
  Stream<Link> outgoingLinks(LinkType type);

  /**
   * All links lead only to the root page. Sections have no incoming links. If {@param type} is
   * {@link LinkType#SECTION_LINKS} then it returns this section's incoming links. Otherwise it
   * returns incoming links for the root page
   *
   * @param type which links to return
   * @return links to this section (or its root page)
   */
  @NotNull
  Stream<Link> incomingLinks(LinkType type);

  default int incomingLinksCount(LinkType type) {
    return (int) incomingLinks(type).count();
  }

  default int outgoingLinksCount(LinkType type) {
    return (int) outgoingLinks(type).count();
  }

  @NotNull
  Page parent();

  @NotNull
  Page root();

  boolean isRoot();

  Stream<Page> subpages();

  Stream<List<Term>> sentences(SegmentType type);

  Vec titleVec();

  Stream<Vec> sentenceVecs();

  interface Link {

    /**
     * @return Title of Target {@code Page}.
     */
    Stream<Term> targetTitle();

    /**
     * @return Text which is displayed for this link.
     */
    Stream<Term> text();

    /**
     * @return {@code true} if Target {@code Page} exists; {@code false} if Target {@code Page} dose
     *     not exist.
     */
    boolean targetExists();

    Page targetPage();

    Page sourcePage();

    /** @return offset by symbols */
    long position();
  }
}
