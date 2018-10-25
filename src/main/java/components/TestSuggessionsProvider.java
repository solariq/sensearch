package components;

import java.util.List;

import components.suggestor.Suggestor;

import java.util.ArrayList;

public class TestSuggessionsProvider implements Suggestor {

  public List<String> getSuggestions(String searchString) {
    ArrayList<String> res = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      res.add(searchString + "a");
    }

    return res;
  }
}
