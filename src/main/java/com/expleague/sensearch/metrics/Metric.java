package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Metric {
  private String googleRequest = "https://www.google.ru/search?q=Википедия:";
  private Path pathToMetrics;
  private UserAgents userAgents = new UserAgents();

  public Metric(Path pathToMetric) throws IOException {
    pathToMetrics = pathToMetric;
    Files.createDirectories(pathToMetrics);
  }

  private List<String> getCookies() throws IOException {
    URL urlG = new URL("https://www.google.ru/");
    URLConnection con = urlG.openConnection();

    return con.getHeaderFields().get("Set-Cookie");
  }

  private void setCookies(URLConnection urlConnection) throws IOException {
    List<String> cookies = getCookies();
    for (String cookie : cookies) {
      urlConnection.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
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

  private List<String> getGoogleTitles(Integer size, String query) throws IOException {
    List<String> result = new ArrayList<>();
    while (result.size() < size) {
      String request = googleRequest + query.replace(" ", "%20")
          + "&start=" + result.size();
      URL url = new URL(request);
      URLConnection connection = url.openConnection();
      setCookies(connection);
      userAgents.setAnyAgent(connection);

      BufferedReader br = new BufferedReader(
          new InputStreamReader(connection.getInputStream()));
      boolean check = false;

      String line;
      while ((line = br.readLine()) != null) {
        Matcher matcher = Pattern.compile("<h3 class=\"LC20lb\">.*?</h3>").matcher(line);
        while (matcher.find()) {
          String title = normalizeTitle(matcher.group(0));
          if (title != null) {
            check = true;
            result.add(title);
          }
        }
      }
      br.close();

      if (!check) {
        break;
      }
    }
    return null;
  }

  public void calculate(String query, ResultItem[] resultItems) throws IOException {

    List<String> ourTitles = new ArrayList<>();
    for (ResultItem r : resultItems) {
      ourTitles.add(r.title().toString());
    }

    List<String> googleTitles = getGoogleTitles(ourTitles.size(), query);

  }

}
