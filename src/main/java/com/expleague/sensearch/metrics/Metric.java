package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Metric {

  private final String googleRequest = "https://www.google.ru/search?q=site:ru.wikipedia.org%20";
  private Path pathToMetrics;
  private UserAgents userAgents = new UserAgents();
  private final String MAP_FILE = "MAP.json";
  private final String METRIC_FILE = "METRIC";
  private Set<String> allTitles;

  public Metric(Path pathToMetric) {
    pathToMetrics = pathToMetric;
    try {
      Files.createDirectories(pathToMetrics);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Metric() {
  }

  public void pushTitles(Set<String> allTitles) {
    this.allTitles = allTitles;
  }

  private List<String> getCookies() throws IOException {
    URL urlG = new URL("https://www.google.ru/");
    URLConnection connection = urlG.openConnection();

    return connection.getHeaderFields().get("Set-Cookie");
  }

  private void setCookies(URLConnection urlConnection) throws IOException {
    List<String> cookies = getCookies();
    if (cookies != null) {
      for (String cookie : cookies) {
        urlConnection.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
      }
    }
  }

  private String normalizeTitle(String title) {
    String ans = title
        .replace("<h3 class=\"LC20lb\">", "")
        .replace("</h3>", "");
    if (ans.endsWith(" — Википедия")) {
      ans = ans.replace(" — Википедия", "");
      return ans;
    }
    return null;
  }

  private List<ResultItem> getGoogleResults(Integer size, String query) {
    List<ResultItem> results = new ArrayList<>();

    while (results.size() < size) {
      try {
        String request = googleRequest + query.replace(" ", "%20")
            + "&start=" + results.size();
        URL url = new URL(request);
        URLConnection connection = url.openConnection();
        setCookies(connection);
        userAgents.setAnyAgent(connection);

        Document document = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());

        Elements googleSnippets = document.select("div.g");
        googleSnippets.forEach(element -> {
          String title = normalizeTitle(element.select("h3.LC20lb").text());
          if (title == null) {
            return;
          }

          String snippet = element.select("span.st").text();
          String snippetUrl = element.select("a[href]").attr("href");
          try {
            results.add(new ResultItemImpl(new URI(snippetUrl), title,
                Arrays.asList(new Pair<>(snippet, new ArrayList<>())), 0));
          } catch (URISyntaxException e) {
            e.printStackTrace();
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return results;
  }

  public ResultItem[] calculate(String query, Page[] resultItems) {

    List<String> ourTitles = new ArrayList<>();
    for (Page r : resultItems) {
      ourTitles.add(r.title().toString());
    }
    Path tmpPath = pathToMetrics.resolve(String.valueOf(query));
    List<ResultItem> googleResults = new ArrayList<>();

    if (true) {
      try {
        Files.createDirectories(tmpPath);
      } catch (IOException e) {
        System.err.println("Can't create directory: " + query);
      }

      googleResults = getGoogleResults(ourTitles.size(), query);
      ObjectMapper objectMapper = new ObjectMapper();
//      try {
//        objectMapper.writeValue(tmpPath.resolve(MAP_FILE).toFile(), googleResults);
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
    } else {
      ObjectMapper objectMapper = new ObjectMapper();

      TypeReference<List<ResultItem>> mapType = new TypeReference<List<ResultItem>>() {
      };

      try {
        googleResults = objectMapper.readValue(tmpPath.resolve(MAP_FILE).toFile(), mapType);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    double DCG = 0.0;
    int ind = 0;
    for (String title : ourTitles) {
      ResultItem googleResult = googleResults.stream()
          .filter(item -> item.title().equals(title)).findFirst()
          .orElse(null);
      if (googleResult == null) {
        continue;
      }
      double numDouble = googleResults.indexOf(googleResult) + 1;
      numDouble = 1.0 / numDouble;
      DCG += numDouble / (Math.log(2 + ind) / Math.log(2));
      ind++;
    }

    System.err.println("Query: " + query + " DCG: " + DCG);
    try (BufferedWriter DCGWriter = new BufferedWriter(
        new OutputStreamWriter(
            Files.newOutputStream(tmpPath.resolve(METRIC_FILE))))) {
      DCGWriter.write(String.valueOf(DCG));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return googleResults.toArray(new ResultItem[0]);
  }

  void calculateRebase(String query, Page[] pages) {
  }
}
