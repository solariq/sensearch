package components;

import components.suggestor.Suggestor;
import java.util.ArrayList;
import java.util.List;

public class TestSuggessionsProvider implements Suggestor {

  public List<String> getSuggestions(String searchString) {
    ArrayList<String> res = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      res.add(searchString + "a");
    }

    return res;
  }
}
