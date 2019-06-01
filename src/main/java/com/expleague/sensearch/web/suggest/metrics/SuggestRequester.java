package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class SuggestRequester {
  //public final String suggestUrl = "https://suggest.yandex.ru/suggest-ya.cgi?v=4&part=";

  public final String suggestUrl = "https://www.google.com/complete/search?client=opera&q=";
  public List<String> getSuggests(String partialQuery) throws IOException {
    URL url = new URL(suggestUrl + URLEncoder.encode(partialQuery));

    ObjectMapper mapper = new ObjectMapper();

    List<String> suggests = new ArrayList<String>();

    URLConnection connection =  url.openConnection();
    
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);

    JsonNode root = mapper.readTree(connection.getInputStream());

    Iterator<JsonNode> it = root.get(1).iterator();
    while(it.hasNext()) {
      suggests.add(it.next().asText().trim());
    }

    return suggests;
  }

  public static void main(String[] args) throws IOException {
    SuggestRequester requester = new SuggestRequester();

    List<String> results = requester.getSuggests("wiki алек");
    System.out.println(results.size());
    for (String suggest : results) {
      System.out.println(suggest);
    }

  }
}
