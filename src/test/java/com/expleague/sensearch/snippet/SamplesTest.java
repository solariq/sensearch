package com.expleague.sensearch.snippet;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.LogBasedMyStem;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class SamplesTest {

  private SnippetsCreator sc = new SnippetsCreator();
  private Index index = new Index() {
    @Override
    public Stream<Page> fetchDocuments(Query query) {
      return null;
    }

    @Override
    public Term[] synonyms(Term term) {
      return new Term[0];
    }

    @Override
    public boolean hasTitle(CharSequence title) {
      return false;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public int vocabularySize() {
      return 0;
    }

    @Override
    public double averagePageSize() {
      return 0;
    }

    @Override
    public int documentFrequency(Term term) {
      return 0;
    }

    @Override
    public long termFrequency(Term term) {
      return 0;
    }
  };
  private Lemmer lemmer;

  private List<Page> pages = new ArrayList<>();
  private List<Query> queries = new ArrayList<>();

  @Rule
  public TestName testName = new TestName();

  @Before
  public void prepare() {
    MyStem myStem = new LogBasedMyStem(
//          Paths.get("./resources/mystem"),
        Paths.get("myStemTestLogs", this.getClass().getName() + "_" + testName.getMethodName()));
    lemmer = new Lemmer(myStem);

    File folder = new File("./src/test/java/com/expleague/sensearch/snippet/samples");
    Arrays.stream(folder.listFiles()).sorted().forEach(fileEntry -> {
      if (fileEntry.isDirectory()) {
        try {
          CharSequence content = String
              .join("", Files.readAllLines(Paths.get(fileEntry.getPath() + "/content")));
          CharSequence query = String
              .join("", Files.readAllLines(Paths.get(fileEntry.getPath() + "/query")));
          pages.add(new Page() {
            @Override
            public URI reference() {
              return null;
            }

            @Override
            public CharSequence title() {
              return null;
            }

            @Override
            public CharSequence text() {
              return content;
            }
          });
          queries.add(new BaseQuery(query, index, lemmer));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

  }


  @Test
  public void test() {
    for (int i = 0; i < pages.size(); i++) {
      Snippet snippet = sc.getSnippet(pages.get(i), queries.get(i), lemmer);
      System.out.println(snippet.getContent());
      System.out.println();
    }
  }
}
