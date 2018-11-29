package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.snippet.Segment;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UnitMetricTest {

  private Metric metric;
  private WebCrawler crawler;

  private double log2(int ind) {
    return (Math.log(1 + ind) / Math.log(2));
  }

  private double readDCG() {
    double DCG = -1;
    try (BufferedReader reader =
        Files.newBufferedReader(
            Paths.get("./src/test/java/com/expleague/sensearch/metrics/TMP")
                .resolve("test")
                .resolve("METRIC"))) {
      DCG = Double.valueOf(reader.readLine());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return DCG;
  }

  @Before
  public void init() {
    crawler = new Crawler();
    metric = new Metric(crawler, Paths.get("./src/test/java/com/expleague/sensearch/metrics/TMP"));
  }

  @Test
  public void equalsTest() {
    List<Page> res = new ArrayList<>();
    res.add(new PageTest("Test1"));
    res.add(new PageTest("Test2"));
    res.add(new PageTest("Test3"));
    res.add(new PageTest("Test4"));
    res.add(new PageTest("Test5"));
    res.add(new PageTest("Test6"));
    res.add(new PageTest("Test7"));
    res.add(new PageTest("Test8"));
    res.add(new PageTest("Test9"));
    res.add(new PageTest("Test10"));

    metric.calculate("test", res.toArray(new Page[0]));
    double DCG = readDCG();

    double rightDCG =
        ((1.0 / 1) / (log2(1)))
            + ((1.0 / 2) / (log2(2)))
            + ((1.0 / 3) / (log2(3)))
            + ((1.0 / 4) / (log2(4)))
            + ((1.0 / 5) / (log2(5)))
            + ((1.0 / 6) / (log2(6)))
            + ((1.0 / 7) / (log2(7)))
            + ((1.0 / 8) / (log2(8)))
            + ((1.0 / 9) / (log2(9)))
            + ((1.0 / 10) / (log2(10)));

    Assert.assertEquals(DCG, rightDCG, 1e-10);
  }

  @Test
  public void emptyTest() {
    List<Page> res = new ArrayList<>();
    res.add(new PageTest("Test01"));
    res.add(new PageTest("Test02"));
    res.add(new PageTest("Test03"));
    res.add(new PageTest("Test04"));
    res.add(new PageTest("Test05"));
    res.add(new PageTest("Test06"));
    res.add(new PageTest("Test07"));
    res.add(new PageTest("Test08"));
    res.add(new PageTest("Test09"));
    res.add(new PageTest("Test010"));

    metric.calculate("test", res.toArray(new Page[0]));
    double DCG = readDCG();

    double rightDCG = 0;

    Assert.assertEquals(DCG, rightDCG, 1e-10);
  }

  @Test
  public void randAllTest() {
    List<Page> res = new ArrayList<>();
    res.add(new PageTest("Test1"));
    res.add(new PageTest("Test7"));
    res.add(new PageTest("Test10"));
    res.add(new PageTest("Test2"));
    res.add(new PageTest("Test6"));
    res.add(new PageTest("Test4"));
    res.add(new PageTest("Test8"));
    res.add(new PageTest("Test3"));
    res.add(new PageTest("Test9"));
    res.add(new PageTest("Test5"));

    metric.calculate("test", res.toArray(new Page[0]));
    double DCG = readDCG();

    double rightDCG =
        ((1.0 / 1) / (log2(1)))
            + ((1.0 / 7) / (log2(2)))
            + ((1.0 / 10) / (log2(3)))
            + ((1.0 / 2) / (log2(4)))
            + ((1.0 / 6) / (log2(5)))
            + ((1.0 / 4) / (log2(6)))
            + ((1.0 / 8) / (log2(7)))
            + ((1.0 / 3) / (log2(8)))
            + ((1.0 / 9) / (log2(9)))
            + ((1.0 / 5) / (log2(10)));

    Assert.assertEquals(DCG, rightDCG, 1e-10);
  }

  @Test
  public void randPartTest() {
    List<Page> res = new ArrayList<>();
    res.add(new PageTest("Test100"));
    res.add(new PageTest("Test7"));
    res.add(new PageTest("Test10"));
    res.add(new PageTest("Test2"));
    res.add(new PageTest("Test6"));
    res.add(new PageTest("Test04"));
    res.add(new PageTest("Test8"));
    res.add(new PageTest("Test3"));
    res.add(new PageTest("Test09"));
    res.add(new PageTest("Test5"));

    metric.calculate("test", res.toArray(new Page[0]));
    double DCG = readDCG();

    double rightDCG =
        0.0
            + ((1.0 / 7) / (log2(2)))
            + ((1.0 / 10) / (log2(3)))
            + ((1.0 / 2) / (log2(4)))
            + ((1.0 / 6) / (log2(5)))
            + 0.0
            + ((1.0 / 8) / (log2(7)))
            + ((1.0 / 3) / (log2(8)))
            + 0.0
            + ((1.0 / 5) / (log2(10)));

    Assert.assertEquals(DCG, rightDCG, 1e-10);
  }

  @After
  public void clear() throws IOException {
    FileUtils.deleteDirectory(
        Paths.get("./src/test/java/com/expleague/sensearch/metrics/TMP").toFile());
  }

  private class Crawler implements WebCrawler {

    @Override
    public List<ResultItem> getGoogleResults(Integer size, String query) {
      List<ResultItem> list = new ArrayList<>();
      list.add(new ResultItemTest("Test1"));
      list.add(new ResultItemTest("Test2"));
      list.add(new ResultItemTest("Test3"));
      list.add(new ResultItemTest("Test4"));
      list.add(new ResultItemTest("Test5"));
      list.add(new ResultItemTest("Test6"));
      list.add(new ResultItemTest("Test7"));
      list.add(new ResultItemTest("Test8"));
      list.add(new ResultItemTest("Test9"));
      list.add(new ResultItemTest("Test10"));
      return list;
    }

    @Override
    public void setPath(Path pathToMetric) {}
  }

  private class ResultItemTest implements ResultItem {

    private String title;

    public ResultItemTest(String title) {
      this.title = title;
    }

    @Override
    public URI reference() {
      return null;
    }

    @Override
    public CharSequence title() {
      return title;
    }

    @Override
    public List<Pair<CharSequence, List<Segment>>> passages() {
      return null;
    }

    @Override
    public double score() {
      return 0;
    }
  }

  private class PageTest implements Page {

    private String title;

    public PageTest(String t) {
      title = t;
    }

    @Override
    public URI reference() {
      return null;
    }

    @Override
    public CharSequence title() {
      return title;
    }

    @Override
    public CharSequence text() {
      return null;
    }
  }
}
