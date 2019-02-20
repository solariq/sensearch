package com.expleague.sensearch.core.impl.json;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.snippet.Segment;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ResultItemDeserializer extends StdDeserializer<ResultItem> {

  public ResultItemDeserializer() {
    this(null);
  }

  protected ResultItemDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public ResultItem deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    ObjectCodec codec = jsonParser.getCodec();
    JsonNode node = codec.readTree(jsonParser);
    try {
      URI reference = new URI(node.get("reference").asText());
      String title = node.get("title").asText();
      List<Pair<CharSequence, List<Segment>>> passages = new ArrayList<>();
      node.get("passages")
          .elements()
          .forEachRemaining(
              n -> {
                String text = n.get("text").asText();
                List<Segment> highlights = new ArrayList<>();
                n.get("highlights")
                    .elements()
                    .forEachRemaining(
                        s -> {
                          List<Integer> oneSegment = new ArrayList<>();
                          s.elements().forEachRemaining(os -> oneSegment.add(os.intValue()));
                          highlights.add(new Segment(oneSegment.get(0), oneSegment.get(1)));
                        });
                passages.add(new Pair<>(text, highlights));
              });

      //      int score = node.get("score").intValue();
      //      ResultItemDebugInfo debugInfo =
      //          codec.treeToValue(node.get("debugInfo"), ResultItemDebugInfo.class);
      return new ResultItemImpl(reference, title, passages, 0, null);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return null;
  }
}
