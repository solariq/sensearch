package com.expleague.sensearch.searcher;

import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.query.BaseQuery;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.Test;

public class FuzzySearcherTest {
//
//  @Test
//  public void testRelevantDocumentIsOnTop() {
//    IndexedPage relevantDocument = new IndexedPage() {
//      @Override
//      public long id() {
//        return 0;
//      }
//
//      @Override
//      public CharSequence getContent() {
//        return "а а а а слово еще а а слово и еще одно слово в документе";
//      }
//
//      @Override
//      public CharSequence getTitle() {
//        return "странный тупой заголовок";
//      }
//    };
//
//    IndexedPage irrelevantDocument = new IndexedPage() {
//      @Override
//      public long id() {
//        return 0;
//      }
//
//      @Override
//      public CharSequence getContent() {
//        return "нет слов в документе";
//      }
//
//      @Override
//      public CharSequence getTitle() {
//        return "еще один странный тупой заголовок";
//      }
//    };
//
//    SenSeArch searcher = new FuzzySearcher(query -> Stream.<IndexedPage>builder()
//        .add(irrelevantDocument)
//        .add(relevantDocument)
//        .build(), 4);
//
//    assertEquals(Arrays.asList(relevantDocument),
//        searcher.getRankedDocuments(new BaseQuery("слово")));
//  }
}