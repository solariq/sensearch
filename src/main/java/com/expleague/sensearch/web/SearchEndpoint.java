package com.expleague.sensearch.web;

import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
public class SearchEndpoint {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final SenSeArch search;
  private final Suggestor suggestor;

  // Note: this is javax @Inject, not Guice's as Jersey uses HK2 DI under the hood
  @Inject
  public SearchEndpoint(Builder builder) {
    search = builder.getSearcher();
    suggestor = builder.getSuggestor();
  }

  @GET
  @Path("/suggest")
  @Produces(MediaType.APPLICATION_JSON)
  public String suggest(@DefaultValue("") @QueryParam("query") String query)
      throws JsonProcessingException {
    return mapper.writeValueAsString(suggestor.getSuggestions(query));
  }

  @GET
  @Path("/search")
  @Produces(MediaType.APPLICATION_JSON)
  public String search(
      @DefaultValue("") @QueryParam("query") String query,
      @DefaultValue("0") @QueryParam("page") int pageNumber)
      throws JsonProcessingException {
    return mapper.writeValueAsString(search.search(query, pageNumber));
  }

  //  @GET
  //  @Produces(MediaType.TEXT_HTML)
  //  public String index() throws IOException {
  //    return String.join("\n", Files.readAllLines(Paths.get(Config.getMainPageHTML())));
  //  }

}
