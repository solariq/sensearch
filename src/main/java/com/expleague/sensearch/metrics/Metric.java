package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Metric {

  private final String googleRequest = "https://www.google.ru/search?q=site:ru.wikipedia.org%20";
  private Path pathToMetrics;
  private UserAgents userAgents = new UserAgents();
  private final String MAP_FILE = "MAP";
  private final String METRIC_FILE = "METRIC";

  public Metric(Path pathToMetric) {
    pathToMetrics = pathToMetric;
    try {
      Files.createDirectories(pathToMetrics);
    } catch (IOException e) {
      e.printStackTrace();
    }
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

  private Map<String, Integer> getGoogleTitles(Integer size, String query) {
    Map<String, Integer> answer = new HashMap<>();
    Integer ourSize = 0;
    while (ourSize < size) {
      try {
        String request = googleRequest + query.replace(" ", "%20")
            + "&start=" + ourSize;
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
            ourSize++;
            if (title != null) {
              check = true;
              answer.put(title, ourSize);
            }
          }
        }
        br.close();
        if (!check) {
          break;
        }
      } catch (IOException ignored) {
      }
    }
    return answer;
  }

  public void calculate(String query, ResultItem[] resultItems) {

    List<String> ourTitles = new ArrayList<>();
    for (ResultItem r : resultItems) {
      ourTitles.add(r.title().toString());
    }
    Path tmpPath = pathToMetrics.resolve(String.valueOf(query));
    Map<String, Integer> googleTitles = new HashMap<>();

    if (!Files.exists(tmpPath.resolve(MAP_FILE))) {
      try {
        Files.createDirectories(tmpPath);
      } catch (IOException e) {
        System.err.println("Can't create directory: " + query);
      }

      googleTitles = getGoogleTitles(ourTitles.size(), query);
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        objectMapper.writeValue(tmpPath.resolve(MAP_FILE).toFile(), googleTitles);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      ObjectMapper objectMapper = new ObjectMapper();

      TypeReference<Map<String, Integer>> mapType = new TypeReference<Map<String, Integer>>() {
      };

      try {
        googleTitles = objectMapper.readValue(tmpPath.resolve(MAP_FILE).toFile(), mapType);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Double DCG = 0.0;
    int ind = 0;
    for (String title : ourTitles) {
      Integer num = googleTitles.get(title);
      double numDouble = 0;
      if (num != null) {
        numDouble = 1.0 / num;
      }
      DCG += numDouble / (Math.log(2 + ind) / Math.log(2));
      ind++;
    }

    System.err.println(DCG);
    try (BufferedWriter DCGWriter = new BufferedWriter(
        new OutputStreamWriter(
            Files.newOutputStream(tmpPath.resolve(METRIC_FILE))))) {
      DCGWriter.write(String.valueOf(DCG));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
