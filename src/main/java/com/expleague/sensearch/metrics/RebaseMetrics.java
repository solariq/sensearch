package com.expleague.sensearch.metrics;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.core.impl.ResultPageImpl;
import com.expleague.sensearch.snippet.Segment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;

public class RebaseMetrics {

  public static void main(String[] args) throws IOException, XMLStreamException, URISyntaxException {
    ObjectMapper objectMapper = new ObjectMapper();
    List<Segment> segs1 = new ArrayList<>();
    segs1.add(new Segment(1, 2));
    segs1.add(new Segment(3, 4));
    List<Segment> segs0 = new ArrayList<>();
    segs0.add(new Segment(10, 20));
    segs0.add(new Segment(30, 40));

    List<Pair<CharSequence, List<Segment>>> pas = new ArrayList<>();
    pas.add(new Pair<>("A", segs1));
    pas.add(new Pair<>("B", segs0));
    List<Pair<CharSequence, List<Segment>>> pasG = new ArrayList<>();
    pasG.add(new Pair<>("B", segs0));
    pasG.add(new Pair<>("A", segs1));

    ResultItem resultItem = new ResultItemImpl(new URI("reference"),
        "title", pas, 0);
    ResultItem resultItemG = new ResultItemImpl(new URI("referenceGoogle"),
        "titleGoogle", pasG, 0);
    ResultItem[] res1 = new ResultItem[2];
    res1[0] = resultItem;
    res1[1] = resultItemG;
    ResultItem[] res2 = new ResultItem[2];
    res2[0] = resultItemG;
    res2[1] = resultItem;

    ResultPage resultPage = new ResultPageImpl(
        0,
        1,
        res1,
        res2
    );
    objectMapper.writeValue(Paths.get("test.json").toFile(), resultPage);

    ResultPage resultItem1 = objectMapper.readValue(Paths.get("test.json").toFile(), ResultPage.class);
    System.err.println(resultItem1);
  }

}
