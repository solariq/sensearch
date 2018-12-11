package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.index.Index;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class RequestCrawler implements WebCrawler {

  private final String googleRequest = "https://www.google.ru/search?q=site:ru.wikipedia.org%20";
  private UserAgents userAgents = new UserAgents();
  private Path pathToMetric;
  private Map<String, String> cookies = null;

  private final Index index;

  public RequestCrawler(Index index) {
    this.index = index;
  }

  private void setCookies(Map<String, String> cookies, URLConnection urlConnection) {

    String resultCookie =
        cookies
            .entrySet()
            .stream()
            .map(cookie -> cookie.getKey() + "=" + cookie.getValue())
            .collect(Collectors.joining("; "));
    urlConnection.setRequestProperty("Cookie", resultCookie);
  }

  private void updateCookies(Map<String, String> cookies, List<String> newCookieStr) {
    newCookieStr.forEach(
        newCookie -> {
          String[] tokens = newCookie.split("=");
          cookies.put(
              tokens[0],
              String.join("=", Arrays.stream(tokens, 1, tokens.length).collect(Collectors.toList()))
                  .split(";")[0]);
        });
  }

  private String normalizeTitle(String title) {
    String ans = title;
    if (ans.endsWith(" — Википедия")) {
      return ans.replace(" — Википедия", "");
    }
    return null;
  }

  @Override
  public List<ResultItem> getGoogleResults(Integer size, String query) throws IOException {
    if (cookies == null) {
      cookies = getDefaultCookies();
    }

    List<ResultItem> results = new ArrayList<>();

    Random random = new Random();

    int page = 0;
    final int[] article = {0};

    while (results.size() < size && page < 5) {
      page++;
      String request = googleRequest + query.replace(" ", "%20") + "&start=" + article[0];
      URL url = new URL(request);
      URLConnection connection = url.openConnection();
      setCookies(cookies, connection);
      userAgents.setAnyAgent(connection);

      Document document = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());

      if (connection.getHeaderField("Set-Cookie") != null) {
        System.out.println(String.join("\n", connection.getHeaderFields().get("Set-Cookie")));
        updateCookies(cookies, connection.getHeaderFields().get("Set-Cookie"));
      }

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
            final URI uri;
            try {
              uri = new URI(URLDecoder.decode(snippetUrl));
            } catch (URISyntaxException e) {
              e.printStackTrace();
              return;
            }
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
      try {
        Thread.sleep((int) (3000 * random.nextDouble()));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return results;
  }

  private Map<String, String> getDefaultCookies() throws IOException {
    URLConnection urlConnection = new URL("http://google.com").openConnection();
    urlConnection.connect();

    Map<String, String> cookies = new HashMap<>();
    updateCookies(cookies, urlConnection.getHeaderFields().get("Set-Cookie"));
    return cookies;
  }

  @Override
  public void setPath(Path pathToMetric) {
    this.pathToMetric = pathToMetric;
  }
}
