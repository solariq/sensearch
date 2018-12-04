package com.expleague.sensearch;

import java.net.URI;
import java.util.List;
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
   * @return Page text
   */
  @NotNull
  CharSequence text();

  /**
   * @return List of categories this page or empty list if page have no categories
   */
  @NotNull
  List<CharSequence> categories();

  /**
   * @return List of {@link Section} this page
   */
  @NotNull
  List<Section> sections();

  /**
   * @return List of referring Page
   */
  List<Page> inputLinks();


  interface Section {

    /**
     * @return Section text
     */
    @NotNull
    CharSequence text();

    /**
     * @return Title of this section. As there can be sections, sub-sections and so on, this method
     *     returns a list of parents titles. For example, for subsection C in section B in artile A
     *     it will return [A, B, C]
     */
    @NotNull
    List<CharSequence> title();

    /** @return {@link Link}s that are contained in this section */
    @NotNull
    List<Link> links();

  }

  /** Link to another {@link com.expleague.sensearch.Page} */
  interface Link {

    /** @return Text which is displayed for this link */
    @NotNull
    CharSequence text();

    /**
     * @return Sentence with this link
     */
    @NotNull
    CharSequence context();

    /**
     * @return referenced Page
     */
    Page targetPage();

    /** @return Where this link starts in its sentence */
    int textOffset();
  }
}
