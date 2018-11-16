package com.expleague.sensearch.core.deserializers;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.snippet.Segment;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ResultItemDeserializer extends StdDeserializer<ResultItem> {

  public ResultItemDeserializer() {
    this(null);
  }

  protected ResultItemDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ResultItem deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    try {
      URI reference = new URI(node.get("reference").asText());
      String title = node.get("title").asText();
      List<Pair<CharSequence, List<Segment>>> passages;

      //return new ResultItemImpl(reference, title, passages, 0);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return null;
  }
}
