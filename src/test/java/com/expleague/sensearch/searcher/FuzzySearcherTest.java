package com.expleague.sensearch.searcher;

import static org.junit.Assert.assertEquals;

import com.expleague.sensearch.FuzzySearcher;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.index.IndexedDocument;
import com.expleague.sensearch.query.BaseQuery;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.Test;

public class FuzzySearcherTest {

  @Test
  public void testRelevantDocumentIsOnTop() {
    IndexedDocument relevantDocument = new IndexedDocument() {
      @Override
      public long getId() {
        return 0;
      }

      @Override
      public CharSequence getContent() {
        return "а а а а слово еще а а слово и еще одно слово в документе";
      }

      @Override
      public CharSequence getTitle() {
        return "странный тупой заголовок";
      }
    };

    IndexedDocument irrelevantDocument = new IndexedDocument() {
      @Override
      public long getId() {
        return 0;
      }

      @Override
      public CharSequence getContent() {
        return "нет слов в документе";
      }

      @Override
      public CharSequence getTitle() {
        return "еще один странный тупой заголовок";
      }
    };

    SenSeArch searcher = new FuzzySearcher(query -> Stream.<IndexedDocument>builder()
        .add(irrelevantDocument)
        .add(relevantDocument)
        .build(), 4);

    assertEquals(Arrays.asList(relevantDocument),
        searcher.getRankedDocuments(new BaseQuery("слово")));
  }
}