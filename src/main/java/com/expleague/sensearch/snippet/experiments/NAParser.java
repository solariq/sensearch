package com.expleague.sensearch.snippet.experiments;

import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.expleague.sensearch.snippet.experiments.naturalquestions.LongAnswer;
import com.expleague.sensearch.snippet.experiments.naturalquestions.Result;
import com.expleague.sensearch.snippet.experiments.naturalquestions.ShortAnswer;
import com.expleague.sensearch.snippet.experiments.pool.QueryAndPassages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NAParser {

    private static Tokenizer tokenizer = new TokenizerImpl();

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
        return URI.create("https://en.wikipedia.org/wiki/" + pageUri);
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 3) {
            System.err.println("Wrong arguments. Usage : <path to na json> <action : 0 - data for ranking, 1 - data for snippets> <path to save data>");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Result[] results = objectMapper.readValue(Paths.get(args[0]).toFile(), Result[].class);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        List<QueryAndPassages> queryAndPassages = new ArrayList<>();
        List<QueryAndResults> queryAndResults = new ArrayList<>();

        for (int i = 0; i < results.length; i++) {
            Result result = results[i];

            switch (args[1]) {
                case "0": { // for learning
                    String query = result.getQuestion_text();
                    String uri = createUriForTitle(result.getDocument_title()).toString();
                    PageAndWeight pw = new PageAndWeight(uri, 1.0);
                    queryAndResults.add(new QueryAndResults(query, Collections.singletonList(pw)));
                }
                break;
                case "1":
                    if (result.getAnnotations().size() > 0
                            && result.getAnnotations().get(0).getShort_answers().size() > 0) {
                        String query = result.getQuestion_text();
                        String uri = createUriForTitle(result.getDocument_title()).toString();

                        final String html = result.getDocument_html();
                        byte[] htmlBytes = html.getBytes();

                        LongAnswer longAnswer = result.getAnnotations().get(0).getLong_answer();
                        final int lLong = longAnswer.getStart_byte();
                        final int rLong = longAnswer.getEnd_byte();
                        byte[] longAnsBytes = Arrays.copyOfRange(htmlBytes, lLong, rLong);
                        String longAnsString = eraseLinks(new String(longAnsBytes));

                        ShortAnswer shortAnswer = result.getAnnotations().get(0).getShort_answers().get(0);
                        final int l = shortAnswer.getStart_byte();
                        final int r = shortAnswer.getEnd_byte();
                        byte[] ans = Arrays.copyOfRange(htmlBytes, l, r);
                        String shortAnsString = eraseLinks(new String(ans));

                        List<CharSequence> passages = tokenizer.toSentences(longAnsString).collect(Collectors.toList());
                        List<QueryAndPassages.PassageAndWeight> passageAndWeights = passages.stream()
                                .filter(passage -> passage.toString().contains(shortAnsString))
                                .map(passage -> new QueryAndPassages.PassageAndWeight(uri, passage.toString(), 1))
                                .collect(Collectors.toList());


                        queryAndPassages.add(new QueryAndPassages(query, passageAndWeights));
                    }
                    break;
                default:
                    return;
            }
        }

        switch (args[1]) {
            case "0":
                objectMapper.writeValue(new File(args[2]), queryAndResults);
                break;
            case "1":
                objectMapper.writeValue(new File(args[2]), queryAndPassages);
                break;
            default:
        }
    }
}
