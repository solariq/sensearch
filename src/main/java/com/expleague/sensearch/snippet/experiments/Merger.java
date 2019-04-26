package com.expleague.sensearch.snippet.experiments;

import com.expleague.sensearch.snippet.experiments.pool.QueryAndPassages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Merger {

    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        List<QueryAndPassages> filtered = new ArrayList<>();

        for (int i = 0; i < 50; ++i) {
            String path = "data/snippets-data-" + String.format("%02d", i) + ".json";
            QueryAndPassages[] results = objectMapper.readValue(Paths.get(path).toFile(), QueryAndPassages[].class);
            filtered.addAll(Arrays.stream(results).filter(x -> x.answers().length > 0).collect(Collectors.toList()));
        }

        objectMapper.writeValue(new File("snippets-data.json"), filtered);
    }

}
