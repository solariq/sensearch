package com.expleague.sensearch.snippet.experiments;

import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NAParser {

  public static String eraseLinks(String s) {
    return s
        .replaceAll("<.*?>", "")
        .replaceAll("&quot;", "\"")
        .replaceAll("&#39;", "'")
        .replaceAll("\\[[0-9]*?]", "")
        .trim();
  }

  private static URI createUriForTitle(String title) {
    String pageUri = URLEncoder.encode(title.replace(" ", "_").replace("%", "%25"));
    return URI.create("https://ru.wikipedia.org/wiki/" + pageUri);
  }

  public static void main(String[] args) throws Exception {
    byte[] jsonData = Files.readAllBytes(Paths.get("./resources/nq-train-sample.json"));

    ObjectMapper objectMapper = new ObjectMapper();
//    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    Result[] results = objectMapper.readValue(jsonData, Result[].class);

    List<Data> datas = new ArrayList<>();
    List<QueryAndResults> queryAndResults = new ArrayList<>();

    for (int i = 0; i < results.length; i++) {
      Result result = results[i];

      { // for learning
        String query = result.getQuestion_text();
        String uri = createUriForTitle(result.getDocument_title()).toString();
        PageAndWeight pw = new PageAndWeight(uri, 1.0);
        queryAndResults.add(new QueryAndResults(query, Collections.singletonList(pw)));
      }

      if (result.getAnnotations().size() > 0
          && result.getAnnotations().get(0).getShort_answers().size() > 0) {
        String title = result.getDocument_title();
        String query = result.getQuestion_text();
        final String html = result.getDocument_html();
        byte[] htmlBytes = html.getBytes();

        LongAnswer longAnswer = result.getAnnotations().get(0).getLong_answer();
        final int lLong = longAnswer.getStart_byte();
        final int rLong = longAnswer.getEnd_byte();
        byte[] longAnsBytes = Arrays.copyOfRange(htmlBytes, lLong, rLong);
        String longAnsString = eraseLinks(new String(longAnsBytes));

        ShortAnswer shortAnswer = result.getAnnotations().get(0).getShort_answers().get(0);
        final int l = shortAnswer.getStart_byte();;
        final int r = shortAnswer.getEnd_byte();
        byte[] ans = Arrays.copyOfRange(htmlBytes, l, r);;
        String shortAnsString = eraseLinks(new String(ans));

        Data data = new Data();
        data.setTitle(title);
        data.setQuery(query);
        data.setShort_answer(shortAnsString);
        data.setLong_answer(longAnsString);

        datas.add(data);
      }
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    mapper.writeValue(new File("./src/main/java/com/expleague/sensearch/snippet/experiments/data.json"), datas);
    mapper.writeValue(new File("./PoolData/na.json"), queryAndResults);
  }
}
