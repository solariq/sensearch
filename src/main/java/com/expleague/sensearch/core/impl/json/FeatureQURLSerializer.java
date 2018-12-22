package com.expleague.sensearch.core.impl.json;

import com.expleague.sensearch.miner.impl.QURLItem;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;

public class FeatureQURLSerializer extends StdSerializer<QURLItem> {

  protected FeatureQURLSerializer(Class<QURLItem> t) {
    super(t);
  }

  public FeatureQURLSerializer() {
    this(null);
  }

  @Override
  public void serialize(QURLItem QURLItem, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartObject();

    jsonGenerator.writeObjectField("query", QURLItem.query());
    jsonGenerator.writeObjectField("page", QURLItem.page());

    jsonGenerator.writeEndObject();
  }
}
