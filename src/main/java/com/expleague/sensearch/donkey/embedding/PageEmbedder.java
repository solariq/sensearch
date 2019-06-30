package com.expleague.sensearch.donkey.embedding;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.sensearch.donkey.randomaccess.ProtoTermIndex;
import com.expleague.sensearch.donkey.randomaccess.ProtoTermStatsIndex;
import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory;
import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory.SerializedTextHelper;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PageEmbedder {

  private final SerializedTextHelperFactory helperFactory;
  private final ProtoTermStatsIndex statsIndex;
  private final Embedding<CharSeq> embedding;
  private final ProtoTermIndex termIndex;

  public PageEmbedder(
      ProtoTermStatsIndex statsIndex,
      ProtoTermIndex termIndex,
      SerializedTextHelperFactory helperFactory,
      Embedding<CharSeq> embedding) {
    this.statsIndex = statsIndex;
    this.termIndex = termIndex;
    this.helperFactory = helperFactory;
    this.embedding = embedding;
  }

  public EmbeddedPage embed(Page page) {
    SerializedTextHelper titleHelper = helperFactory.helper(page.getTitle());
    final Vec titleVec = getAverage(titleHelper.termIdsArray());
    List<Vec> titleVecs = titleVec == null ? Collections.emptyList() : Collections.singletonList(titleVec);

    SerializedTextHelper textHelper = helperFactory.helper(page.getContent());
    final Vec textVec = getAverage(textHelper.termIdsArray());
    List<Vec> textVecs = textVec == null ? Collections.emptyList() : Collections.singletonList(titleVec);

    List<Vec> linkVecs = new ArrayList<>();
    page.getOutgoingLinksList().forEach(link -> {
      SerializedTextHelper linkHelper = helperFactory.helper(link.getText());
      Vec linkVec = getAverage(linkHelper.termIdsArray());
      if (linkVec != null) {
        linkVecs.add(linkVec);
      }
    });

    return new EmbeddedPage(page.getPageId(), titleVecs, textVecs, linkVecs);
  }

  private Vec getAverage(int[] termIds) {
    if (termIds.length == 0) {
      return null;
    }

    Vec vec = new ArrayVec(DEFAULT_VEC_SIZE);
    for (int i : termIds) {
      double idf = 1.0 / Objects.requireNonNull(statsIndex.value(i)).getDocumentFrequency();
      CharSeq text = CharSeq.create(Objects.requireNonNull(termIndex.value(i)).getText());
      VecTools.incscale(vec, Objects.requireNonNull(embedding.apply(text)), idf / termIds.length);
    }

    return vec;
  }
}
