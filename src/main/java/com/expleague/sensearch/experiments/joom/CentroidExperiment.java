package com.expleague.sensearch.experiments.joom;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.index.Index;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gnu.trove.list.TIntList;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CentroidExperiment {

  private Tokenizer tokenizer = new TokenizerImpl();
  private Injector injector = Guice.createInjector(new AppModule(CrawlerJoom.class));
  private Index index = injector.getInstance(Index.class);

  public CentroidExperiment() throws IOException {
  }

  public static void main(String[] args) throws Exception {
    new CentroidExperiment().experiment();
  }

  public void experiment() {

    Page goodPage1 =
        index.page(
            URI.create("https://www.joom.com/en/products/1468304746000092933-123-1-582-614756762"));
    Page goodPage2 =
        index.page(
            URI.create("https://www.joom.com/en/products/1461905174556485304-200-1-553-255781398"));

    Vec queryVec = index.vecByTerms(index.parse("галстук").collect(Collectors.toList()));
    System.out.println(
        cosineDist(
            queryVec,
            vecWithIdf(
                index.parse("finest neckties for men genius").collect(Collectors.toList()))));

    Page badPage1 =
        index.page(
            URI.create("https://www.joom.com/en/products/1461317518772674666-21-1-553-1473875305"));
    Page badPage2 =
        index.page(
            URI.create("https://www.joom.com/en/products/1461905219846984671-94-1-553-762024713"));

    List<Vec> vecs = fiveGramVecs(goodPage2);
    vecs.forEach(System.out::println);
    List<TIntList> cluster = new QtCluster(0.75).cluster(vecs);
    for (TIntList tIntList : cluster) {
      System.out.println(tIntList);
    }
    /*
    Stream<Vec> vecStream =
        index.allDocuments().map(this::fiveGramVecs).flatMap(Collection::stream);

    Vec[] centroids = VecTools.kMeans(5, vecStream);


    for (Vec v : centroids) {
      System.out.println(v);
    }
    //    vecs.forEach(vec -> System.out.println(cosineDist(vec, queryVec)));
    System.out.println("========");

    System.out.println(queryVec);

    mostDistantVecs(centroids, fiveGramVecs(goodPage1), 5)
        .forEach(vec -> System.out.println(cosineDist(vec, queryVec)));
    System.out.println("=====");

    mostDistantVecs(centroids, fiveGramVecs(goodPage2), 5)
        .forEach(vec -> System.out.println(cosineDist(vec, queryVec)));
    System.out.println("=====\n=====");

    mostDistantVecs(centroids, fiveGramVecs(badPage1), 5)
        .forEach(vec -> System.out.println(cosineDist(vec, queryVec)));
    System.out.println("=====");

    mostDistantVecs(centroids, fiveGramVecs(badPage2), 5)
        .forEach(vec -> System.out.println(cosineDist(vec, queryVec)));

    //    System.out.println(cosineDist(closestVecs(centroids, fiveGramVecs(goodPage)), queryVec));
    //    System.out.println(cosineDist(mostDistantVec(centroids, fiveGramVecs(badPage)),
    // queryVec));
    System.out.println("kek");*/
  }

  private double cosineDist(Vec v1, Vec v2) {
    return (1 - VecTools.cosine(v1, v2)) / 2;
  }

  private Vec closestVecs(Vec[] vecs, Vec vec) {
    return Arrays.stream(vecs).min(Comparator.comparing(v -> cosineDist(v, vec))).get();
  }

  private Vec mostDistantVec(Vec[] vecs, Vec vec) {
    return Arrays.stream(vecs).min(Comparator.comparing(v -> -cosineDist(v, vec))).get();
  }

  private List<Vec> mostDistantVecs(Vec[] centroids, List<Vec> vecs, int cnt) {
    return vecs.stream()
        .sorted(Comparator.comparing(vec -> -cosineDist(mostDistantVec(centroids, vec), vec)))
        .limit(cnt)
        .collect(Collectors.toList());
  }

  private List<Vec> fiveGramVecs(Page page) {
    List<Vec> vecs = fiveGramVecs(page.content(SegmentType.FULL_TITLE));
    vecs.addAll(fiveGramVecs(page.content(SegmentType.BODY)));
    return vecs;
  }

  private List<Vec> fiveGramVecs(CharSequence text) {
    List<Term> tokens = index.parse(text).collect(Collectors.toList());

    List<Vec> result = new ArrayList<>();

    for (int i = 0; i < tokens.size() - 5; i++) {

      Vec vec = vecWithIdf(tokens.subList(i, i + 5));
      if (!VecTools.equals(vec, new ArrayVec(vec.dim()))) {
        result.add(vec);
      }
    }

    return result;
  }

  private Vec vecWithIdf(List<Term> tokens) {
    Vec vec = new ArrayVec(300);
    tokens.forEach(
        token -> {
          VecTools.incscale(
              vec,
              index.vecByTerms(Collections.singletonList(token)),
              1.0 / Math.log(1.0 * (1 + token.documentFreq())));
        });
    return vec;
  }
}
