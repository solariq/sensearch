package com.expleague.sensearch.web.suggest;

import java.util.List;

public interface Suggestor {

  public List<String> getSuggestions(String searchString);
}
