package com.expleague.sensearch.core.deserializers;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class ResultPageDeserializer extends StdDeserializer<ResultPage> {

  public ResultPageDeserializer() {
    this(null);
  }

  protected ResultPageDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ResultPage deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    int number = node.get("number").intValue();
    int totalResults = (Integer) node.get("totalResults").numberValue();
    ResultItem[] results;
    ResultItem[] googleResults;
    return null;
    //return new ResultPageImpl(number, totalResults, results, googleResults);
  }
}
