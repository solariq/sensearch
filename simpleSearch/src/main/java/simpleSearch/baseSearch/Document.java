package simpleSearch.baseSearch;

/**
 * Created by sandulmv on 04.10.18.
 */
public class Document {
  private CharSequence rawContent;

  public Document(CharSequence rawContent) {

    this.rawContent = rawContent;
  }

  public CharSequence getRawContent() {
    return rawContent;
  }
}
