package com.expleague.sensearch.core.deserializers;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.core.impl.ResultPageImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultPageDeserializer extends StdDeserializer<ResultPage> {

  public ResultPageDeserializer() {
    this(null);
  }

  protected ResultPageDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ResultPage deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    ObjectCodec codec = jsonParser.getCodec();
    JsonNode node = codec.readTree(jsonParser);
    ResultItemDeserializer resultItemDeserializer = new ResultItemDeserializer();
    List<ResultItem> results = new ArrayList<>();
    List<ResultItem> googleResults = new ArrayList<>();

    for (JsonNode result : node.get("results")) {
      results.add(codec.treeToValue(result, ResultItem.class));
    }

    for (JsonNode result : node.get("googleResults")) {
      googleResults.add(codec.treeToValue(result, ResultItem.class));
    }

    return new ResultPageImpl(
        0, 0, results.toArray(new ResultItem[0]), googleResults.toArray(new ResultItem[0]));
  }
}
