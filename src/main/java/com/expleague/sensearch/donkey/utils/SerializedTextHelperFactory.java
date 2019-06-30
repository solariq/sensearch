package com.expleague.sensearch.donkey.utils;

import com.expleague.sensearch.donkey.randomaccess.ProtoTermIndex;
import com.expleague.sensearch.donkey.term.TokenParser;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.SerializedText;
import java.util.Objects;
import java.util.stream.IntStream;

public class SerializedTextHelperFactory {

  private final ProtoTermIndex termIndex;

  public SerializedTextHelperFactory(ProtoTermIndex termIndex) {
    this.termIndex = termIndex;
  }

  public SerializedTextHelper helper(SerializedText serializedText) {
    return new SerializedTextHelper(serializedText);
  }

  public class SerializedTextHelper {

    private final SerializedText serializedText;
    private final int[] tokenIds;

    private SerializedTextHelper(SerializedText serializedText) {
      this.serializedText = serializedText;
      this.tokenIds = serializedText.getTokenIdsList().stream()
          .mapToInt(Integer::intValue)
          .toArray();
    }

    public SerializedText serializedText() {
      return serializedText;
    }

    public int[] tokenIds() {
      return tokenIds;
    }

    public IntStream termIdsStream() {
      return IntStream.of(tokenIds)
          .filter(TokenParser::isWord)
          .map(TokenParser::toId);
    }

    public IntStream lemmaIdsStream() {
      return IntStream.of(tokenIds)
          .filter(TokenParser::isWord)
          .map(TokenParser::toId)
          .map(idx -> Objects.requireNonNull(termIndex.value(idx)).getLemmaId());
    }
  }
}
