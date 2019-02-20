package com.expleague.sensearch.core.impl.json;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.snippet.Segment;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;

public class ResultItemSerializer extends StdSerializer<ResultItem> {

  protected ResultItemSerializer(Class<ResultItem> t) {
    super(t);
  }

  public ResultItemSerializer() {
    this(null);
  }

  @Override
  public void serialize(
      ResultItem resultItem, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    jsonGenerator.writeStartObject();

    jsonGenerator.writeStringField("reference", resultItem.reference().toString());
    jsonGenerator.writeStringField("title", resultItem.title().toString());

    jsonGenerator.writeArrayFieldStart("passages");
    for (Pair<CharSequence, List<Segment>> passage : resultItem.passages()) {
      jsonGenerator.writeStartObject();
      jsonGenerator.writeStringField("text", passage.first.toString());

      jsonGenerator.writeArrayFieldStart("highlights");
      for (Segment highlight : passage.second) {
        jsonGenerator.writeArray(new int[]{highlight.getLeft(), highlight.getRight()}, 0, 2);
      }
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();
    }
    jsonGenerator.writeEndArray();

    jsonGenerator.writeNumberField("score", resultItem.score());

    jsonGenerator.writeObjectField("debugInfo", resultItem.debugInfo());

    jsonGenerator.writeEndObject();
  }
}
