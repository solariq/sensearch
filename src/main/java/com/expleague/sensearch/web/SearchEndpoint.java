package com.expleague.sensearch.web;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;
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
  // TODO: refactor this
  static {
    SimpleModule module = new SimpleModule();
    module.addSerializer(ResultItem.class, new ResultItemSerializer());
    module.addSerializer(ResultPage.class, new ResultPageSerializer());
    mapper.registerModule(module);
  }

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

  public static class ResultItemSerializer extends StdSerializer<ResultItem> {

    protected ResultItemSerializer(Class<ResultItem> t) {
      super(t);
    }

    public ResultItemSerializer() {
      this(null);
    }

    @Override
    public void serialize(
        ResultItem resultItem,
        JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
      jsonGenerator.writeStartObject();

      jsonGenerator.writeStringField("reference", resultItem.reference().toString());
      jsonGenerator.writeStringField("title", resultItem.title().toString());

      jsonGenerator.writeArrayFieldStart("passages");
      for (Pair<CharSequence, List<Segment>> passage : resultItem.passages()) {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("text", passage.first.toString());

        jsonGenerator.writeArrayFieldStart("highlights");
        for (Segment highlight: passage.second) {
          jsonGenerator.writeArray(new int[] {highlight.getLeft(), highlight.getRight()}, 0, 2);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
      }
      jsonGenerator.writeEndArray();

      jsonGenerator.writeEndObject();
    }
  }

  public static class ResultPageSerializer extends StdSerializer<ResultPage> {

    protected ResultPageSerializer(Class<ResultPage> t) {
      super(t);
    }

    public ResultPageSerializer() {
      this(null);
    }

    @Override
    public void serialize(ResultPage resultPage, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
      jsonGenerator.writeStartObject();

      jsonGenerator.writeObjectField("results", resultPage.results());
      jsonGenerator.writeObjectField("googleResults", resultPage.googleResults());

      jsonGenerator.writeEndObject();
    }
  }

}
