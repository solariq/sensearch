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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
//    List<String> cookies = getCookies();
//    if (cookies != null) {
//      String resultCookie = cookies.stream().map(cookie -> cookie.split(";", 2)[0]).collect(Collectors.joining("; "));
      urlConnection.setRequestProperty("Cookie", "CGIC=IlV0ZXh0L2h0bWwsYXBwbGljYXRpb24veGh0bWwreG1sLGFwcGxpY2F0aW9uL3htbDtxPTAuOSxpbWFnZS93ZWJwLGltYWdlL2FwbmcsKi8qO3E9MC44; SID=kwaUY0kEqptz9WqPlab4nKKtGX18uLV0aR0ufs0o26UibWQAU2RhyWjLHKNMFo7gVzRopA.; HSID=AXsi5ijI32tE6k_Vh; SSID=A4hbAX4sT7y35ap8m; APISID=d26GGAI_W37s3KQH/AR7OVpPKlDBwgUroD; SAPISID=vg_3cm0PN1vNcUyC/AOptCxN_Pim17voio; OGPC=19009353-1:; GOOGLE_ABUSE_EXEMPTION=ID=46c7579afed41c63:TM=1542817514:C=r:IP=195.144.231.198-:S=APGng0u81ezICXj4MZFoUZzeNxsUTZlqCQ; NID=146=PGdy2A-3XgUTzzeLIa4YMaNYYGCxiaSAuy3gEEFZ-g4tC-YkBlfNn-eMPgKIZ3xXMmxPVZIqpEBTqaUhRCZkJJm6WtYYj8Jp2HiMxWcs8cZeLgpXlyhDQnGnqHgcIC9YtFb_umdD-j814nIgi2tN4FVy__qIXCXX0_JhJFtqqDDfDA0Njs2r5cSel-B_1tYoPfadVa2yDRV91EuF-jJSOpyf1NWCbBeIIEcrY43odqXK8OmTDPdctjI; 1P_JAR=2018-11-21-17; DV=02U1xCMR5kJTQCeZhV-tOAGGNwN2cxbWSxWyBWS9OAEAADBIhHf0405rqAAAACwaSbD5clGrSgAAAIfP0zVEMDWdGQAAAAx`");
//    }
  }

  private String normalizeTitle(String title) {
    String ans = title;
    if (ans.endsWith(" — Википедия")) {
      ans = ans.replace(" — Википедия", "");
      if (index.hasTitle(ans)) {
        return ans;
      }
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
        String request = googleRequest + query.replace(" ", "%20")
            + "&start=" + article[0];
        URL url = new URL(request);
        URLConnection connection = url.openConnection();
        setCookies(connection);
        userAgents.setAnyAgent(connection);

        Document document = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());

        Elements googleSnippets = document.select("div.g");
        googleSnippets.forEach(element -> {
          String title = normalizeTitle(element.select("h3.LC20lb").text());
          article[0]++;
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
  public void setPath(Path pathToMetric) {
    this.pathToMetric = pathToMetric;
  }
}
