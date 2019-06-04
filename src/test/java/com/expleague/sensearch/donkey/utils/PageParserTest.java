package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class PageParserTest {

  @Test
  public void SimpleTest() {
    PageParser parser = new PageParser();
    String text = "Hello, world!";
    List<CharSeq> expected = new ArrayList<>();
    expected.add(CharSeq.create("Hello"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("world"));
    expected.add(CharSeq.create("!"));
    List<CharSeq> ans = parser.parse(CharSeq.create(text));
    Assert.assertEquals(expected, ans);
  }


  @Test
  public void HardTest() {
    PageParser parser = new PageParser();
    String text = "\"heLLo, privEt!23xyi,_ %@()\\\".;@ kl\t \\n wo'w\"";
    List<CharSeq> expected = new ArrayList<>();
    expected.add(CharSeq.create("\""));
    expected.add(CharSeq.create("heLLo"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("privEt"));
    expected.add(CharSeq.create("!"));
    expected.add(CharSeq.create("23xyi"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create("_"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("%"));
    expected.add(CharSeq.create("@"));
    expected.add(CharSeq.create("("));
    expected.add(CharSeq.create(")"));
    expected.add(CharSeq.create("\\"));
    expected.add(CharSeq.create("\""));
    expected.add(CharSeq.create("."));
    expected.add(CharSeq.create(";"));
    expected.add(CharSeq.create("@"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("kl"));
    expected.add(CharSeq.create("\t"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("\\"));
    expected.add(CharSeq.create("n"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("wo"));
    expected.add(CharSeq.create("'"));
    expected.add(CharSeq.create("w"));
    expected.add(CharSeq.create("\""));
    List<CharSeq> ans = parser.parse(CharSeq.create(text));
    Assert.assertEquals(expected, ans);
  }

  @Test
  public void StrangeTest() {
    PageParser parser = new PageParser();
    String text = "!@#$%№^&*()_+=-1234567890 \n \t qwer-tyuiop[]asd1fghjkl;'zxcv0bnm,./";
    List<CharSeq> expected = new ArrayList<>();
    expected.add(CharSeq.create("!"));
    expected.add(CharSeq.create("@"));
    expected.add(CharSeq.create("#"));
    expected.add(CharSeq.create("$"));
    expected.add(CharSeq.create("%"));
    expected.add(CharSeq.create("№"));
    expected.add(CharSeq.create("^"));
    expected.add(CharSeq.create("&"));
    expected.add(CharSeq.create("*"));
    expected.add(CharSeq.create("("));
    expected.add(CharSeq.create(")"));
    expected.add(CharSeq.create("_"));
    expected.add(CharSeq.create("+"));
    expected.add(CharSeq.create("="));
    expected.add(CharSeq.create("-"));
    expected.add(CharSeq.create("1234567890"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("\n"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("\t"));
    expected.add(CharSeq.create(" "));
    expected.add(CharSeq.create("qwer"));
    expected.add(CharSeq.create("-"));
    expected.add(CharSeq.create("tyuiop"));
    expected.add(CharSeq.create("["));
    expected.add(CharSeq.create("]"));
    expected.add(CharSeq.create("asd1fghjkl"));
    expected.add(CharSeq.create(";"));
    expected.add(CharSeq.create("'"));
    expected.add(CharSeq.create("zxcv0bnm"));
    expected.add(CharSeq.create(","));
    expected.add(CharSeq.create("."));
    expected.add(CharSeq.create("/"));
    List<CharSeq> ans = parser.parse(CharSeq.create(text));
    Assert.assertEquals(expected, ans);
  }
}
