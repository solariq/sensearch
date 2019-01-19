package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainPage;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
  private final Random random = new Random();
  private Path pathToMetric;
  private Map<String, Map<String, String>> cookies = new HashMap<>();

  private final Index index;
  private static final List<String> agentsList = new ArrayList<>();
  private static final List<String> dummyRequests = new ArrayList<>();

  static {
    agentsList.add("Mozilla/5.0 (Windows NT 6.3; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.101 Safari/537.36 OPR/40.0.2308.62");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E; rv:11.0) like Gecko");
    agentsList.add("Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; ASU2JS; rv:11.0) like Gecko");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586");
    agentsList.add(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.67 Safari/537.36");

    dummyRequests.add("путин");
    dummyRequests.add("владимир владимирович");
    dummyRequests.add("фмл 239");
    dummyRequests.add("сколько лет алле пугачевой");
    dummyRequests.add("президент сша");
    dummyRequests.add("сборная белоруссии по футболу");
    dummyRequests.add("купить видеокарту спб");
    dummyRequests.add("информационный поиск");
    dummyRequests.add("ACM ICPC 2019");
    dummyRequests.add("Balalaika");
  }

  @Inject
  public RequestCrawler(Index index) {
    this.index = index;
    agentsList.forEach(agent -> cookies.put(agent, null));
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

  private Map<String, String> getDefaultCookies(String userAgent) throws IOException {
    URLConnection urlConnection = new URL("http://google.com").openConnection();
    urlConnection.setRequestProperty("User-Agent", userAgent);
    urlConnection.connect();

    Map<String, String> cookies = new HashMap<>();
    updateCookies(cookies, urlConnection.getHeaderFields().get("Set-Cookie"));
    return cookies;
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


  private Document makeRequest(String request, String userAgent) throws IOException {
    URL url = new URL(request);
    URLConnection connection = url.openConnection();
    connection.setRequestProperty("User-Agent", userAgent);
    setCookies(cookies.get(userAgent), connection);

    Document document = Jsoup.parse(connection.getInputStream(), "UTF-8", url.toString());

    if (connection.getHeaderField("Set-Cookie") != null) {
      //System.out.println(userAgent);
      //System.out.println(String.join("\n", connection.getHeaderFields().get("Set-Cookie")));
      updateCookies(cookies.get(userAgent), connection.getHeaderFields().get("Set-Cookie"));
    }

    return document;
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
    String userAgent = agentsList.get(random.nextInt(agentsList.size()));
    if (cookies.get(userAgent) == null) {
      //      cookies.put(userAgent, new HashMap<>());
      //      updateCookies(
      //          cookies.get(userAgent),
      //          Arrays.asList(
      //
      // "CGIC=IlV0ZXh0L2h0bWwsYXBwbGljYXRpb24veGh0bWwreG1sLGFwcGxpY2F0aW9uL3htbDtxPTAuOSxpbWFnZS93ZWJwLGltYWdlL2FwbmcsKi8qO3E9MC44; SID=kwaUY0kEqptz9WqPlab4nKKtGX18uLV0aR0ufs0o26UibWQAU2RhyWjLHKNMFo7gVzRopA.; HSID=AXsi5ijI32tE6k_Vh; SSID=A4hbAX4sT7y35ap8m; APISID=d26GGAI_W37s3KQH/AR7OVpPKlDBwgUroD; SAPISID=vg_3cm0PN1vNcUyC/AOptCxN_Pim17voio; OGPC=19009353-1:; 1P_JAR=2018-12-11-11; NID=150=RuiUyuzjxvWOYCN4qOUOYUQeaYCFT193E-unrc6m33ri-rxWkdJzFN445FwNtlZm4-ESNtyMgz9X7ywm4pgVh_wmoN0IM4g4jqtVaamydWZqf70MAh_pddXcOnqN2-k-OX4xtIxp5LwSNzyGe-UB_wXIL4HPRZ1yKwjZ32EBmRfHOQwT26eWjBcsCNg5dajl_iXVs-K0snT1g7CUkTUgeofHdOou9if51_Rz7zM9kgauz7dHZSvl2QLKBxU; DV=02U1xCMR5kJTQCeZhV-tOAHWFeTRedbh83QNEUxNZwQAAID1UoVsAVkvbgEAAAwS4R39uNMaagAAAItGEmy-XNSqIwAAAA"
      //                  .split(";")));
      cookies.put(userAgent, getDefaultCookies(userAgent));
    }

    List<ResultItem> results = new ArrayList<>();

    Random random = new Random();

    int page = 0;
    final int[] article = {0};

    String request = googleRequest + query.replace(" ", "%20");
    while (results.size() < size && page < 5) {
      // Sometimes make a random request not related to Wiki
      if (random.nextInt(3) == 0) {
        makeRequest(
            "https://www.google.ru/search?q="
                + dummyRequests.get(random.nextInt(dummyRequests.size())).replace(" ", "%20"),
            userAgent);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      page++;

      Document document = makeRequest(request, userAgent);

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
              uri = new URI(URLDecoder.decode(snippetUrl, "UTF-8"));
            } catch (URISyntaxException | UnsupportedEncodingException e) {
              e.printStackTrace();
              return;
            }
            if (index.page(uri) == PlainPage.EMPTY_PAGE) {
              return;
            }
            results.add(
                new ResultItemImpl(
                    uri,
                    title,
                    Collections.singletonList(new Pair<>(snippet, new ArrayList<>())),
                    0));
          });

      request = "https://google.ru" + document.select("a.pn[id=pnnext]").attr("href");
      try {
        Thread.sleep((int) (6500 * random.nextDouble()));
      } catch (InterruptedException e) {
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
