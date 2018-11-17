package com.expleague.sensearch.core.deserializers;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.core.impl.ResultPageImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.List;

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
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<List<ResultItem>> typeReference = new TypeReference<List<ResultItem>>() {};
    JsonNode resultsNode = node.get("results");
    List<ResultItem> results = mapper.readValue(resultsNode.traverse(), typeReference);
    List<ResultItem> googleResults = mapper.readValue(node.get("googleResults").traverse(), typeReference);

    return new ResultPageImpl(0, 0,
        results.toArray(new ResultItem[results.size()]),
        googleResults.toArray(new ResultItem[googleResults.size()]));
  }
}
