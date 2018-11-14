package com.expleague.sensearch.index.statistics;

import static org.junit.Assert.assertEquals;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.index.IndexedPage;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;


public class StatisticsTest {

  Config config;
  private IndexedPage d1, d2;

  @Before
  public void prepare() throws JsonGenerationException, JsonMappingException, IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    config = objectMapper.readValue(new File("./config.json"), Config.class);

    d1 = new IndexedPage() {

      @Override
      public String title() {
        return null;
      }

      @Override
      public long id() {
        return 1l;
      }

      @Override
      public CharSequence text() {
        return "a b c d";
      }

      @Override
      public URI reference() {
        // TODO Auto-generated method stub
        return null;
      }
    };
    d2 = new IndexedPage() {

      @Override
      public String title() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public long id() {
        return 2l;
      }

      @Override
      public CharSequence text() {
        return "ab b k";
      }

      @Override
      public URI reference() {
        // TODO Auto-generated method stub
        return null;
      }
    };

    Stats stats = new Stats();
    stats.acceptDocument(d1);
    stats.acceptDocument(d2);

    stats.writeToFile(config.getStatisticsFileName());
  }

  @Test
  public void testCreateSerializeRead()
      throws JsonGenerationException, JsonMappingException, IOException {
    Stats stats = Stats.readStatsFromFile(config.getStatisticsFileName());

    assertEquals(Integer.valueOf(1), stats.getNumberOfDocumentsWithWord().get("a"));
    assertEquals(Integer.valueOf(2), stats.getNumberOfWordOccurences().get("b"));
    assertEquals(Integer.valueOf(1), stats.getTermFrequencyInDocument("b", 2l));

    assertEquals(Integer.valueOf(4), stats.getDocumentLength().get(1l));
    assertEquals(Integer.valueOf(3), stats.getDocumentLength().get(2l));

    assertEquals(2l, stats.getTotalNumberOfDocuments());
    assertEquals(7l, stats.getTotalLength());
  }
}
