package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.web.suggest.pool.LearnedSuggester;
import com.expleague.sensearch.web.suggest.pool.QSUGItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class UsedPhraseLister extends LearnedSuggester {

  public final Set<String> usedStrings = new HashSet<>();
  
  public UsedPhraseLister(Index index, Path suggestIndexRoot) throws IOException {
    super(index, suggestIndexRoot);
  }

  @Override
  public List<QSUGItem> getUnsortedSuggestions(List<Term> terms) throws IOException {
    if (terms.isEmpty()) {
      return Collections.emptyList();
    }


    l:  for (int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue l;
      }

      List<Term> qc = terms.subList(0, terms.size() - intersectLength);
      String qcText = qc.stream().map(Term::text).collect(Collectors.joining(" "));
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);


      List<LookupResult> endingPhrases = suggester.lookup(termsToString(qt), false, 1000000);

      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (LookupResult p : endingPhrases) {
       

        usedStrings.add(qcText);
        usedStrings.add(p.key.toString().toLowerCase());;
      }

    }

    return Collections.emptyList();
  }
  
  public static void main(String[] args) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<String>> map = mapper.readValue(
        Paths.get("sugg_dataset/map").toFile(),
        new TypeReference<Map<String, List<String>>>() {});
    
    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));

    Path suggestRoot = config.getIndexRoot().resolve("suggest");
    Index index = injector.getInstance(Index.class);
    
    UsedPhraseLister lister = new UsedPhraseLister(index, suggestRoot);
    int cnt = 0;
    for (String query : map.keySet()) {
      lister.getUnsortedEndings(query);
      cnt++;
      if (cnt % 1000 == 0) {
        System.err.println(cnt + " queries processed");
      }
    }
    
    PrintWriter out = new PrintWriter(Paths.get("../used_phrases").toFile());
    for (String s : lister.usedStrings) {
      out.println(s);
    }
    out.close();
  }
}
