package com.expleague.sensearch.web;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class PageLoadHandler extends AbstractHandler {

  private final SenSeArch search;
  private final Suggestor suggestor;
  private final Config config;
  private ObjectMapper mapper = new ObjectMapper();

  public PageLoadHandler(
      SenSeArch searcher, BigramsBasedSuggestor bigramsBasedSuggestor, Config config) {
    suggestor = bigramsBasedSuggestor;
    this.config = config;
    this.search = searcher;
  }

  CharSequence generateBoldedText(CharSequence plain, List<Segment> segments) {

    StringBuilder strb = new StringBuilder();
    int left = 0;

    for (Segment segment : segments) {
      strb.append(plain.subSequence(left, segment.getLeft()));
      strb.append("<strong>")
          .append(plain.subSequence(segment.getLeft(), segment.getRight()))
          .append("</strong>");
      left = segment.getRight();
    }

    strb.append(plain.subSequence(left, plain.length()));

    return strb.toString();
  }

  @Override
  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    String requestText = request.getReader().readLine();

    response.setContentType("text/html;charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    baseRequest.setHandled(true);

    final PrintWriter writer = response.getWriter();
    if (requestText == null || requestText.isEmpty()) {

      try (BufferedReader in = new BufferedReader(new FileReader(new File(config.getWebRoot())))) {
        writer.println(in.lines().collect(Collectors.joining("\n")));
      }

      String searchString = request.getParameter("searchForm");
      String pageNo = request.getParameter("page");
      int page = pageNo != null ? Integer.parseInt(pageNo) : 0;
      if (searchString != null) {
        writer.println("<br>Результаты по запросу \"" + searchString + "\":<br><br>");

        final SenSeArch.ResultPage serp = this.search.search(searchString, page);

        final SenSeArch.ResultItem[] results = serp.results();
        for (int i = 0; i < results.length; i++) {
          writer.println("<br><strong>" + (i + 1) + ". " + results[i].title() + "</strong><br>");
          results[i]
              .passages()
              .stream()
              .map(pair -> generateBoldedText(pair.first, pair.second))
              .forEach(writer::println);
        }
        writer.println("</body></html>");
      }
    } else {
      writer.println(mapper.writeValueAsString(suggestor.getSuggestions(requestText)));
    }
  }
}
