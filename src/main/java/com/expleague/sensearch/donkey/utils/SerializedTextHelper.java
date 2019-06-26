package com.expleague.sensearch.donkey.utils;

import com.expleague.sensearch.donkey.randomaccess.RandomAccess;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.SerializedText;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.stream.IntStream;

public class SerializedTextHelper {
  private SerializedText serializedText;
  private int[] tokenIds;

  private final TIntIntMap termIdToLemmaIdMap;
  public SerializedTextHelper(RandomAccess<Term> termIndex) {
    termIdToLemmaIdMap = new TIntIntHashMap();
    termIndex.forEach(term -> termIdToLemmaIdMap.put(term.getId(), term.getLemmaId()));
  }

  public void setSerializedText(SerializedText serializedText) {
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
