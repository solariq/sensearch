package com.expleague.sensearch.core.impl.json;

import com.expleague.sensearch.SenSeArch.ResultItemDebugInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class ResultItemDebugInfoSerializer extends StdSerializer<ResultItemDebugInfo> {

  protected ResultItemDebugInfoSerializer(Class<ResultItemDebugInfo> t) {
    super(t);
  }

  public ResultItemDebugInfoSerializer() {
    this(null);
  }

  @Override
  public void serialize(
      ResultItemDebugInfo debugInfo,
      JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider)
      throws IOException {

    jsonGenerator.writeStartObject();
    jsonGenerator.writeNumberField("rank", debugInfo.rank());

    jsonGenerator.writeFieldName("features");
    jsonGenerator.writeArray(debugInfo.features(), 0, debugInfo.features().length);

    jsonGenerator.writeArrayFieldStart("featureIds");
    for (String id : debugInfo.featureIds()) {
      jsonGenerator.writeString(id);
    }
    jsonGenerator.writeEndArray();

    jsonGenerator.writeStringField("uri", debugInfo.uri());

    jsonGenerator.writeEndObject();
  }
}
