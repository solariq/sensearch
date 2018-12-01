package com.expleague.sensearch.donkey.crawler.document;

import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;

public interface CrawlerDocument {

  CharSequence content();

  String title();

  List<String> categories();

  List<Section> sections();

  long iD();

  URI uri();

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

    /** @return Links that are contained in this section */
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
     * @return Id of wikipedia article this lini references. Equals to -1 if there is no article for
     *     this link or this link leads to some other resource (for example, image)
     */
    long targetId();

    /** @return Where this link starts in its section text */
    int textOffset();
  }
}
