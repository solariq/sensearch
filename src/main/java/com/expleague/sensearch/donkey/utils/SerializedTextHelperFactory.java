package com.expleague.sensearch.donkey.utils;

import com.expleague.sensearch.core.TokenIdUtils;
import com.expleague.sensearch.donkey.randomaccess.ProtoTermIndex;
import com.expleague.sensearch.donkey.randomaccess.RandomAccess;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.SerializedText;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import java.util.Objects;
import java.util.stream.IntStream;

public class SerializedTextHelperFactory {

  private final RandomAccess<Integer, Term> termIndex;

  public SerializedTextHelperFactory(RandomAccess<Integer, Term> termIndex) {
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
          .filter(TokenIdUtils::isWord)
          .map(TokenIdUtils::toId);
    }

    public int[] termIdsArray() {
      return termIdsStream().toArray();
    }

    public int[] lemmaIdsArray() {
      return lemmaIdsStream().toArray();
    }

    public IntStream lemmaIdsStream() {
      return IntStream.of(tokenIds)
          .filter(TokenIdUtils::isWord)
          .map(TokenIdUtils::toId)
          .map(idx -> Objects.requireNonNull(termIndex.value(idx)).getLemmaId());
    }
  }
}
