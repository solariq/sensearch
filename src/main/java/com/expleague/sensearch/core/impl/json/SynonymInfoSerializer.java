package com.expleague.sensearch.core.impl.json;

import com.expleague.sensearch.SenSeArch.SynonymAndScore;
import com.expleague.sensearch.SenSeArch.SynonymInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class SynonymInfoSerializer extends StdSerializer<SynonymInfo> {

  protected SynonymInfoSerializer(Class<SynonymInfo> t) {
    super(t);
  }

  public SynonymInfoSerializer() {
    this(null);
  }

  @Override
  public void serialize(
      SynonymInfo synonymInfo, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("word", synonymInfo.queryWord());

    jsonGenerator.writeArrayFieldStart("synonyms");
    for (SynonymAndScore synonymAndScore : synonymInfo.queryWordSynonyms()) {
      jsonGenerator.writeStartObject();
      jsonGenerator.writeStringField("word", synonymAndScore.synonym());
      jsonGenerator.writeNumberField("score", synonymAndScore.score());
      jsonGenerator.writeEndObject();
    }
    jsonGenerator.writeEndArray();

    jsonGenerator.writeEndObject();
  }
}
