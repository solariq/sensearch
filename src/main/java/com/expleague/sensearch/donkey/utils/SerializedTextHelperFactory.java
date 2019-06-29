package com.expleague.sensearch.donkey.utils;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page.SerializedText;
import com.expleague.sensearch.term.TermBase;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.stream.IntStream;

public class SerializedTextHelperFactory {

  private final TIntIntMap termIdToLemmaIdMap;

  public SerializedTextHelperFactory(TermBase termBase) {
    termIdToLemmaIdMap = new TIntIntHashMap();
    termBase.stream().forEach(term -> termIdToLemmaIdMap.put(term.getId(), term.getLemmaId()));
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
          .map(termIdToLemmaIdMap::get);
    }
  }
}
