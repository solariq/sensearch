package com.expleague.sensearch.web;

import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.clicks.ClickLogger;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.expleague.sensearch.web.suggest.Suggester;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/")
public class SearchEndpoint {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final SenSeArch search;
  private final Map<String, List<URI>> groundTruthData = new HashMap<>();

  private final Suggester suggester;
  private final ClickLogger clickLogger = new ClickLogger();

  // Note: this is javax @Inject, not Guice's as Jersey uses HK2 DI under the hood
  @Inject
  public SearchEndpoint(SenSeArch search, QueryAndResults[] queryAndResults, Suggester suggester) throws IOException {
    this.search = search;
    for (QueryAndResults queryAndResult : queryAndResults) {
      PageAndWeight[] answers = queryAndResult.getAnswers();
      List<URI> uris =
          Arrays.stream(answers)
              .sorted(Comparator.comparingDouble(PageAndWeight::getWeight).reversed())
              .map(PageAndWeight::getUri)
              .collect(Collectors.toList());
      groundTruthData.put(queryAndResult.getQuery(), uris);
    }
    this.suggester = suggester;
  }

  @GET
  @Path("/suggest")
  @Produces(MediaType.APPLICATION_JSON)
  public String suggest(@DefaultValue("") @QueryParam("query") String query)
      throws JsonProcessingException {
    //return "";
    return mapper.writeValueAsString(suggester.getSuggestions(query));
  }

  @GET
  @Path("/search")
  @Produces(MediaType.APPLICATION_JSON)
  public String search(
      @DefaultValue("") @QueryParam("query") String query,
      @DefaultValue("0") @QueryParam("page") int pageNumber,
      @DefaultValue("false") @QueryParam("debug") boolean debug,
      @DefaultValue("false") @QueryParam("metric") boolean metric)
      throws JsonProcessingException {
    List<ResultItem> dataToDebug = new ArrayList<>();
    if (debug) {
      List<URI> uris = groundTruthData.get(query);
      if (uris != null) {
        dataToDebug =
            uris.stream()
                .map(uri -> new ResultItemImpl(uri, "", Collections.emptyList(), null))
                .collect(Collectors.toList());
      }
    }
    return mapper.writeValueAsString(search.search(query, pageNumber, debug, metric, dataToDebug));
  }

  @GET
  @Path("/query_synonyms")
  @Produces(MediaType.APPLICATION_JSON)
  public String pageSynonyms(
      @QueryParam("uri") String uri, @DefaultValue("") @QueryParam("query") String query)
      throws JsonProcessingException {
    return mapper.writeValueAsString(search.synonyms(uri, query));
  }

  //  @GET
  //  @Produces(MediaType.TEXT_HTML)
  //  public String index() throws IOException {
  //    return String.join("\n", Files.readAllLines(Paths.get(ConfigImpl.getMainPageHTML())));
  //  }

  @POST
  @Path("/clicks")
  public void clicks(
          @DefaultValue("") @QueryParam("session_id") String sessionId,
          @DefaultValue("") @QueryParam("query") String query,
          @DefaultValue("") @QueryParam("uri") String uri) throws IOException {
    clickLogger.log(sessionId, query, uri);
  }

}
