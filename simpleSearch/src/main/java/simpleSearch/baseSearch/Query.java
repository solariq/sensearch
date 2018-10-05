package simpleSearch.baseSearch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sandulmv on 03.10.18.
 */
public class Query {
  private String rawQuery;
  private List<Term> allQueryTerms;

  public Query(String query) {
    allQueryTerms = new ArrayList<>();
    for (String token : query.split("\\s")) {
      allQueryTerms.add(new Term(token.toLowerCase()));
    }
  }

  public List<Term> getTermsList() {
    return allQueryTerms;
  }
}
