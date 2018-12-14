package com.expleague.sensearch;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

public interface Page {
  /**
   * @return Page URI constructed by Page.title
   */
  @NotNull
  URI reference();

  /**
   * @return Page title or "" if page have no title
   */
  @NotNull
  CharSequence title();

  /**
   * @return Page content
   */
  @NotNull
  CharSequence content();

  /**
   * @return List of categories this page or empty list if page have no categories
   */
  @Deprecated
  @NotNull
  List<CharSequence> categories();

  /**
   * @return List of referring Page
   */
  @NotNull
  Stream<Link> outcomingLinks();

  /**
   * @return List of referring Page
   */
  @NotNull
  Stream<Link> incomingLinks();

  /**
   * @return parent ID
   */
  long parent();

  /**
   * @return subpages IDs
   */
  Stream<Link> subpages();

  /** Link to another {@link com.expleague.sensearch.Page} */
  interface Link {

    /** @return Text which is displayed for this link */
    @NotNull
    CharSequence text();

    /**
     * @return referenced Page ID
     */
    long targetPage();

    /**
     * @return source Page ID
     */
    long sourcePage();

    /** @return Where this link starts in its sentence */
    long textOffset();
  }
}
