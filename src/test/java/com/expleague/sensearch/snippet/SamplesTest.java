package com.expleague.sensearch.snippet;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.utils.SensearchTestCase;
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

public class SamplesTest extends SensearchTestCase {

  private SnippetsCreator sc = new SnippetsCreator();

  private Lemmer lemmer;

  private List<Page> pages = new ArrayList<>();
  private List<Query> queries = new ArrayList<>();

  @Rule public TestName testName = new TestName();

  @Before
  public void prepare() {
    MyStem myStem = myStemForTest(SamplesTest.class.getName(), testName.getMethodName());
    lemmer = new Lemmer(myStem);
    File folder = new File("./src/test/java/com/expleague/sensearch/snippet/samples");
    Arrays.stream(folder.listFiles())
        .sorted()
        .forEach(
            fileEntry -> {
              if (fileEntry.isDirectory()) {
                try {
                  CharSequence content =
                      String.join(
                          "", Files.readAllLines(Paths.get(fileEntry.getPath() + "/content")));
                  CharSequence query =
                      String.join(
                          "", Files.readAllLines(Paths.get(fileEntry.getPath() + "/query")));
                  pages.add(
                      new Page() {
                        @Override
                        public URI uri() {
                          return null;
                        }

                        @Override
                        public CharSequence title() {
                          return null;
                        }

                        @Override
                        public CharSequence content() {
                          return content;
                        }

                        @Override
                        public List<CharSequence> categories() {
                          return null;
                        }

                        @Override
                        public Stream<Link> outcomingLinks() {
                          return null;
                        }

                        @Override
                        public Stream<Link> incomingLinks() {
                          return null;
                        }

                        @Override
                        public CharSequence fullContent() {
                          return null;
                        }

                        @Override
                        public Page parent() {
                          return null;
                        }

                        @Override
                        public Stream<Page> subpages() {
                          return null;
                        }
                      });
//                  queries.add(new BaseQuery(query, lemmer));
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
