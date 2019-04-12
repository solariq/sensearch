package com.expleague.sensearch.experiments;

import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NAMerge {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) return;
        String basePath = args[0] + "/na";

        ObjectMapper objectMapper = new ObjectMapper();

        List<QueryAndResults> queryAndResults = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            String path = basePath + String.format("%02d", i) + ".json";
            QueryAndResults[] t = objectMapper.readValue(new File(path), QueryAndResults[].class);
            queryAndResults.addAll(Arrays.asList(t));
        }

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File(args[1]), queryAndResults);
    }
}
