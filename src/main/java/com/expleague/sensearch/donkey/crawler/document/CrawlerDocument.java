package com.expleague.sensearch.donkey.crawler.document;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

public interface CrawlerDocument {

  /**
   * @return Page content
   */
  @NotNull
  CharSequence content();

  /**
   * @return Page titles or "" if page has no titles
   */
  @NotNull
  String title();

  /**
   * @return List of categories of this page or empty list if the page has no categories
   */
  @NotNull
  List<String> categories();

  /**
   * @return List of {@link Section} this page
   */
  @NotNull
  Stream<Section> sections();

  /**
   * @return Page ID or 0 if page has no ID
   */
  @NotNull
  long id();

  /**
   * @return Page URI constructed by Page.titles
   */
  @NotNull
  URI uri();

  interface Section {

    /**
     * @return Section content
     */
    @NotNull
    CharSequence text();

    URI uri();

    /**
     * @return Title of this section. As there can be sections, sub-sections and so on, this method
     *     returns a list of parents titles. For example, for subsection C in section B in article A
     *     it will return [A, B, C]
     */
    @NotNull
    List<CharSequence> titles();

    /**
     * @return Title of this section.
     */
    @NotNull
    CharSequence title();

    /** @return {@link Link}s that are contained in this section */
    @NotNull
    List<Link> links();
  }

  /** Link to another page */
  interface Link {

    /** @return Text which is displayed for this link */
    @NotNull
    CharSequence text();

    /** @return Title of article this link references */
    @NotNull
    CharSequence targetTitle();

    /**
     * @return Id of wikipedia article this link references. Equals to -1 if there is no article for
     *     this link or this link leads to some other resource (for example, image)
     */
    long targetId();

    /**
     * @return Number of characters from section start to link beginning
     */
    int textOffset();
  }
}
