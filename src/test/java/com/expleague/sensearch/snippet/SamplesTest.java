package com.expleague.sensearch.snippet;

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
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class SamplesTest {

  private SnippetsCreator sc = new SnippetsCreator();
  private Index index = new Index() {
    @Override
    public Stream<Page> fetchDocuments(Query query) {
      return null;
    }

    @Override
    public int indexSize() {
      return 0;
    }

    @Override
    public int vocabularySize() {
      return 0;
    }

    @Override
    public double averageWordsPerPage() {
      return 0;
    }

    @Override
    public int pagesWithTerm(Term term) {
      return 0;
    }

    @Override
    public long termCollectionFrequency(Term term) {
      return 0;
    }
  };
  private Lemmer lemmer = new Lemmer(Paths.get("./resources/mystem"));

  private List<Page> pages = new ArrayList<>();
  private List<Query> queries = new ArrayList<>();

  @Before
  public void prepare() {
    File folder = new File("./src/test/java/com/expleague/sensearch/snippet/samples");
    try {
      for (final File fileEntry : folder.listFiles()) {
        if (fileEntry.isDirectory()) {
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
        }
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
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
