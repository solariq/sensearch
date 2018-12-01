package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.index.Index;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class RequestCrawler implements WebCrawler {

  private final String googleRequest = "https://www.google.ru/search?q=site:ru.wikipedia.org%20";
  private UserAgents userAgents = new UserAgents();
  private Path pathToMetric;

  private final Index index;

  public RequestCrawler(Index index) {
    this.index = index;
  }

  private List<String> getCookies() throws IOException {
    URL urlG = new URL("https://www.google.com/");
    URLConnection connection = urlG.openConnection();

    return connection.getHeaderFields().get("Set-Cookie");
  }

  private void setCookies(URLConnection urlConnection) throws IOException {

    List<String> cookies = getCookies();
    if (cookies != null) {
      String resultCookie =
          cookies.stream().map(cookie -> cookie.split(";", 2)[0]).collect(Collectors.joining("; "));
      urlConnection.setRequestProperty("Cookie", resultCookie);
    }
  }

  private String normalizeTitle(String title) {
    String ans = title;
    if (ans.endsWith(" — Википедия")) {
      return ans.replace(" — Википедия", "");
    }
    return null;
  }

  @Override
  public List<ResultItem> getGoogleResults(Integer size, String query) {
    List<ResultItem> results = new ArrayList<>();

    int page = 0;
    final int[] article = {0};
    while (results.size() < size && page < 5) {
      page++;
      try {
        String request = googleRequest + query.replace(" ", "%20") + "&start=" + article[0];
        URL url = new URL(request);
        URLConnection connection = url.openConnection();
        setCookies(connection);
        userAgents.setAnyAgent(connection);

        Document document = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());

        Elements googleSnippets = document.select("div.g");
        googleSnippets.forEach(
            element -> {
              String title = normalizeTitle(element.select("h3.LC20lb").text());
              article[0]++;
              if (title == null) {
                return;
              }
              String snippet = element.select("span.st").text();
              String snippetUrl = element.select("a[href]").attr("href");
              final URI uri = URI.create(URLDecoder.decode(snippetUrl));
              if (index.page(uri) == null) {
                return;
              }
              results.add(
                  new ResultItemImpl(
                      uri,
                      title,
                      Collections.singletonList(new Pair<>(snippet, new ArrayList<>())),
                      0));
            });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return results;
  }

  @Override
  public void setPath(Path pathToMetric) {
    this.pathToMetric = pathToMetric;
  }
}
