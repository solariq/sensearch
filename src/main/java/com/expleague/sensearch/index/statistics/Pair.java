package com.expleague.sensearch.index.statistics;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.io.StringWriter;

public class Pair implements Comparable<Pair> {

  String term;
  long documentID;

  Pair(String s, long l) {
    this.term = s;
    this.documentID = l;
  }

  Pair() {
  }

  @Override
  public String toString() {
    return "[" + term + ", " + documentID + "]";
  }

  @Override
  public int compareTo(Pair o) {

    if (Long.compare(documentID, o.documentID) != 0) {
      return Long.compare(documentID, o.documentID);
    }

    return term.compareTo(o.term);
  }

  @Override
  public boolean equals(Object oth) {
    return this.compareTo((Pair) oth) == 0;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(documentID);
  }

  //auto generated getters and setters
  public String getTerm() {
    return term;
  }

  public void setTerm(String term) {
    this.term = term;
  }

  public long getDocumentID() {
    return documentID;
  }

  public void setDocumentID(int documentID) {
    this.documentID = documentID;
  }

}

class PairSerializer extends JsonSerializer<Pair> {

  private static ObjectMapper mapper = new ObjectMapper();

  @Override
  public void serialize(Pair value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    StringWriter writer = new StringWriter();
    mapper.writeValue(writer, value);
    gen.writeFieldName(writer.toString());
  }

}

class PairDeserializer extends KeyDeserializer {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public Pair deserializeKey(String key, DeserializationContext ctxt) throws IOException {
    return mapper.readValue(key, Pair.class);
  }

}
