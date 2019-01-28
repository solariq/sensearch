package com.expleague.sensearch.web.suggest;

import java.util.List;

public interface Suggestor {

  List<String> getSuggestions(String searchString);
}
