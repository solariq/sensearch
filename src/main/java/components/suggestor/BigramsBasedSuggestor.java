package components.suggestor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.Constants;

public class BigramsBasedSuggestor implements Suggestor {

  private TreeMap<String, Integer> map;

  public BigramsBasedSuggestor(Path filepath) throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    map = mapper.readValue(filepath.toFile(), TreeMap.class);
  }

  public List<String> getSuggestions(String searchString) {
    TreeSet<Entry<String, Integer>> resSet = new TreeSet<>(
        new Comparator<Entry<String, Integer>>() {

          @Override
          public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
            // TODO Auto-generated method stub
            return o1.getValue() - o2.getValue();
          }
        });

    String[] words = searchString.split(Constants.getBigramsRegexp());

    String lastWord = words.length > 0 ? words[words.length - 1].trim() : null;
    String lastBigram = words.length > 1 ?
        words[words.length - 2] + " " + words[words.length - 1]
        : null;

    for (Entry<String, Integer> ent : map.entrySet()) {
      if ((lastWord != null && ent.getKey().startsWith(lastWord))
          || (lastBigram != null && ent.getKey().startsWith(lastBigram))) {
        resSet.add(ent);
      }
    }

    return resSet.stream()
        .map(Entry::getKey)
        .limit(10)
        .collect(Collectors.toList());
  }
}
