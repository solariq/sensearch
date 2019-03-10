package com.expleague.sensearch.snippet.experiments;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

public class NAParser {

  public static String eraseLinks(String s) {
    return s
        .replaceAll("<.*?>", "")
        .replaceAll("&quot;", "\"")
        .replaceAll("&#39;", "'")
        .replaceAll("\\[[0-9]*?]", "")
        .trim();
  }

  public static void main(String[] args) throws Exception {
    byte[] jsonData = Files.readAllBytes(Paths.get("./resources/nq-train-sample.json"));

    ObjectMapper objectMapper = new ObjectMapper();
    Result[] results = objectMapper.readValue(jsonData, Result[].class);

    List<Data> datas = new ArrayList<>();
    for (int i = 0; i < results.length; i++) {
      Result result = results[i];
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

      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(new File("./src/main/java/com/expleague/sensearch/snippet/experiments/data.json"), datas);
    }
  }
}
