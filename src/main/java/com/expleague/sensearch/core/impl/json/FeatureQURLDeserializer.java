package com.expleague.sensearch.core.impl.json;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.miner.impl.QURLItem;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class FeatureQURLDeserializer extends StdDeserializer<QURLItem> {

  public FeatureQURLDeserializer() {
    this(null);
  }

  protected FeatureQURLDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public QURLItem deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
//    ObjectCodec codec = jsonParser.getCodec();
//    JsonNode node = codec.readTree(jsonParser);
//
//    CharSequence query = node.get("query").asText();
//
//    JsonNode pageJSON = node.get("page");
//    Page page = codec.treeToValue(pageJSON, Page.class);
//
    return null; //new QURLItem(query, page);
  }
}
