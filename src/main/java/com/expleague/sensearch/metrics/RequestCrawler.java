package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class RequestCrawler implements WebCrawler {
  private final String googleRequest = "https://www.google.ru/search?q=site:ru.wikipedia.org%20";
  private UserAgents userAgents = new UserAgents();
  private Set<String> allTitles;
  private Path pathToMetric;

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
    String ans = title;
    if (ans.endsWith(" — Википедия")) {
      ans = ans.replace(" — Википедия", "");
      if (allTitles.contains(ans)) {
        return ans;
      }
    }
    return null;
  }

  @Override
  public List<ResultItem> getGoogleResults(Integer size, String query) {
    List<ResultItem> results = new ArrayList<>();

    int page = 0;
    while (results.size() < size && page < 5) {
      page++;
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

  @Override
  public void setAllTitles(Set<String> allTitles) {
    this.allTitles = allTitles;
  }

  @Override
  public void setPath(Path pathToMetric) {
    this.pathToMetric = pathToMetric;
  }
}
