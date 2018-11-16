package com.expleague.sensearch.core.serializers;

import com.expleague.sensearch.SenSeArch.ResultPage;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class ResultPageSerializer extends StdSerializer<ResultPage> {

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