package com.expleague.sensearch.snippet.experiments;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class NAParser {
  public static void main(String[] args) throws IOException {
    byte[] jsonData = Files.readAllBytes(Paths.get("./resources/nq-train-sample.json"));
    ObjectMapper objectMapper = new ObjectMapper();
    Result[] results = objectMapper.readValue(jsonData, Result[].class);

    for (int i = 0; i < results.length; i++) {
      Result result = results[i];
      if (result.getAnnotations().size() > 0 && result.getAnnotations().get(0).getShort_answers().size() > 0) {
        ShortAnswer shortAnswer = result.getAnnotations().get(0).getShort_answers().get(0);
        final String html = result.getDocument_html();
        //System.out.println(html);
        final int l = shortAnswer.getStart_byte();
        final int r = shortAnswer.getEnd_byte();
        //System.out.println(l + " " + r);
        //System.out.println(result.getQuestion_text());
        byte[] htmlBytes = html.getBytes();
        byte[] ans = Arrays.copyOfRange(htmlBytes, l, r);
        String shortAns = new String(ans);
        System.out.println(shortAns + " " + result.getAnnotations().get(0).getYes_no_answer());
      }
      //System.out.println(results.length);
    }
  }
}
