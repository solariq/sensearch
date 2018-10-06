package simpleSearch.queryTmp;

/**
 * Created by sandulmv on 06.10.18.
 */
public class Term {
  private String rawTerm;
  public Term(String rawTerm) {
    this.rawTerm = rawTerm;
  }

  public String getRawTerm() {
    return rawTerm;
  }

  @Override
  public int hashCode() {
    return rawTerm.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Term)) {
      return false;
    }

    Term otherTerm = (Term) other;
    return otherTerm.rawTerm.equals(this.rawTerm);
  }
}
