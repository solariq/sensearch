package com.expleague.sensearch.experiments.joom;

import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class JoomCsvTransformer {

  public void transformData(Path originalDataPath, Path convertedJsonDataPath) throws IOException {
    CSVParser csvRecords =
        CSVParser.parse(originalDataPath, Charset.forName("UTF-8"), CSVFormat.DEFAULT);

    Map<String, Map<String, Integer>> joomQueries = new HashMap<>();
    for (CSVRecord record : csvRecords) {
      if (record.size() != 3) {
        throw new RuntimeException(record.toString());
      }

      String query = record.get(0);
      String id = record.get(1);
      int purchases = Integer.parseInt(record.get(2));

      joomQueries.putIfAbsent(query, new HashMap<>());
      Map<String, Integer> map = joomQueries.get(query);
      map.put(id, map.getOrDefault(id, 0) + purchases);
      //      new PageAndWeight(JoomUtils.uriForId(id).toString(), Math.log(purchases + 2)));
    }

    List<QueryAndResults> data =
        joomQueries
            .entrySet()
            .stream()
            .map(
                x ->
                    new QueryAndResults(
                        x.getKey(),
                        x.getValue()
                            .entrySet()
                            .stream()
                            .map(
                                y ->
                                    new PageAndWeight(
                                        JoomUtils.uriForId(y.getKey()).toString(),
                                        Math.log(y.getValue() + 2)))
                            .collect(Collectors.toList())))
            .collect(Collectors.toList());
    new ObjectMapper()
        .writeValue(Files.newBufferedWriter(convertedJsonDataPath, StandardCharsets.UTF_8), data);
  }
}
