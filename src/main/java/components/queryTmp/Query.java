package components.queryTmp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sandulmv on 06.10.18.
 */
public class Query {
  private List<Term> termsList;
  private String rawQuery;

  public Query(String rawQuery) {
    this.rawQuery = rawQuery;
    termsList = new ArrayList<>();
    for (String token : rawQuery.split("\\s")) {
      termsList.add(new Term(token));
    }
  }

  public List<Term> getTermsList() {
    return termsList;
  }

  public String getRawQuery() {
    return rawQuery;
  }
}
