package com.expleague.sensearch.experiments.joom;

import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    Map<String, List<QueryAndResults.PageAndWeight>> joomQueries = new HashMap<>();
    for (CSVRecord record : csvRecords) {
      String query = record.get(0);
      String id = record.get(1);
      int purchases = Integer.parseInt(record.get(2));

      joomQueries.putIfAbsent(query, new ArrayList<>());
      joomQueries
          .get(query)
          .add(new PageAndWeight(JoomUtils.uriForId(id).toString(), Math.log(purchases + 2)));
      if (record.size() != 3) {
        throw new RuntimeException(record.toString());
      }
    }

    List<QueryAndResults> data =
        joomQueries
            .entrySet()
            .stream()
            .map(x -> new QueryAndResults(x.getKey(), x.getValue()))
            .collect(Collectors.toList());
    new ObjectMapper()
        .writeValue(Files.newBufferedWriter(convertedJsonDataPath, StandardCharsets.UTF_8), data);
  }
}
