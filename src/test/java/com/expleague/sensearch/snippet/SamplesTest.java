//package com.expleague.sensearch.snippet;
//
//import com.expleague.commons.text.lemmer.MyStem;
//import com.expleague.sensearch.Page;
//import com.expleague.sensearch.core.lemmer.MultiLangLemmer;
//import com.expleague.sensearch.query.Query;
//import com.expleague.sensearch.utils.IndexBasedTestCase;
//import java.io.File;
//import java.io.IOException;
//import java.net.URI;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Stream;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.TestName;
//
//public class SamplesTest extends IndexBasedTestCase {
//
//  private SnippetsCreator sc = new SnippetsCreator(index());
//
//  private MultiLangLemmer lemmer;
//
//  private List<Page> pages = new ArrayList<>();
//  private List<Query> queries = new ArrayList<>();
//
//  @Rule public TestName testName = new TestName();
//
//  @Before
//  public void prepare() {
//    MyStem myStem = myStemForTest(SamplesTest.class.getName(), testName.getMethodName());
//    lemmer = new MultiLangLemmer(myStem);
//    File folder = new File("./src/test/java/com/expleague/sensearch/snippet/samples");
//    Arrays.stream(folder.listFiles())
//        .sorted()
//        .forEach(
//            fileEntry -> {
//              if (fileEntry.isDirectory()) {
//                try {
//                  CharSequence content =
//                      String.join(
//                          "", Files.readAllLines(Paths.get(fileEntry.getPath() + "/content")));
//                  CharSequence query =
//                      String.join(
//                          "", Files.readAllLines(Paths.get(fileEntry.getPath() + "/query")));
//                  pages.add(
//                      new Page() {
//                        @Override
//                        public URI uri() {
//                          return null;
//                        }
//
//                        @Override
//                        public CharSequence content(SegmentType... types) {
//                          return content;
//                        }
//
//                        @Override
//                        public List<CharSequence> categories() {
//                          return null;
//                        }
//
//                        @Override
//                        public Stream<Link> outgoingLinks(LinkType type) {
//                          return null;
//                        }
//
//                        @Override
//                        public Stream<Link> incomingLinks(LinkType type) {
//                          return null;
//                        }
//
//                        @Override
//                        public Page parent() {
//                          return null;
//                        }
//
//                        @Override
//                        public Page root() {
//                          return this;
//                        }
//
//                        @Override
//                        public boolean isRoot() {
//                          return true;
//                        }
//
//                        @Override
//                        public Stream<Page> subpages() {
//                          return null;
//                        }
//
//                        @Override
//                        public Stream<CharSequence> sentences(SegmentType type) {
//                          return null;
//                        }
//                      });
//                  // FIXME: parsed terms are needed to for queries
////                  queries.add(new BaseQuery(query));
//                } catch (IOException e) {
//                  e.printStackTrace();
//                }
//              }
//            });
//  }
//
//  @Test
//  @Ignore
//  public void test() {
//    /*for (int i = 0; i < pages.size(); i++) {
//      Snippet snippet = sc.getSnippet(pages.get(i), queries.get(i), lemmer);
//      System.out.println(snippet.getContent());
//      System.out.println();
//    }*/
//  }
//}
