package com.expleague.sensearch.web.suggest.metrics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SuggestsDatasetBuilder {

  public static final String[] citePrefixes = {"wiki ", "вики ", "wikipedia ", "википедия "};

  public static final File f = Paths.get("sugg_dataset/map_google").toFile();

  public static void main(String[] args) throws IOException {
    SuggestRequester sreq = new SuggestRequester();

    ObjectMapper mapper = new ObjectMapper();

    Map<String, Collection<String>> map;
    int loadedQueries = 0;
    if (f.exists()) {
      map = mapper.readValue(
          f, new TypeReference<Map<String, Collection<String>>>() {});

      loadedQueries = map.size();
      System.out.println("Map contained " + loadedQueries + " items");
    } else {
      map = new HashMap<>();
      System.out.println("new map created");
    }
    /*
    Map<String, List<String>> map1 = new HashMap<>();

    map.forEach((k, v) -> {
      map1.put(k.substring(citePrefix.length(), k.length()), v);
    });

    map = map1;
     */
    String[][] queries = Files.lines(Paths.get("wordstat/queries.txt"))
        .map(s -> s.toLowerCase().trim().split("[\\s]+"))
        //.filter(s -> s.length >= 2 && s[1].length() > 0)
        .distinct().toArray(String[][]::new);

    b:    for (String[] s : queries)  {
      String qc = "";
      for (int cw = 0; cw <= 2 && cw < s.length; cw++) {
        for (int i = 1; i < s[cw].length(); i++) {
          String effectiveQuery = qc + s[cw].substring(0, i);
          if (map.containsKey(effectiveQuery)) {
            continue;
          }
          Set<String> allAnswers = new HashSet<>();
          for (String citePrefix : citePrefixes) {
            String realQuery = citePrefix + effectiveQuery;

            try {
              Thread.sleep(200);
              List<String> answers = sreq.getSuggests(realQuery).stream()
                  .filter(s1 -> s1.startsWith(realQuery))
                  .map(s1 -> s1.substring(citePrefix.length(), s1.length()))
                  .collect(Collectors.toList());

              allAnswers.addAll(answers);
              System.out.println(realQuery + " processed");
            } catch (JsonParseException jpe) {
              jpe.printStackTrace();
              continue;
            } catch (IOException e) {
              e.printStackTrace();
              break b;
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          map.put(effectiveQuery, allAnswers);
        }
        qc += s[cw] + " ";
      }
    }

    System.out.println("Number of saved queries: " + map.size());
    System.out.println("Number new queries: " + (map.size() - loadedQueries));

    Files.createDirectories(Paths.get("sugg_dataset"));
    mapper.writeValue(f, map);
  }
}
