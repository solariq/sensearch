package components;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import components.query.BaseQuery;
import components.snippeter.snippet.Cluster;
import components.snippeter.snippet.ClusteredSnippet;
import components.snippeter.snippet.Passage;
import components.snippeter.snippet.Segment;
import components.snippeter.snippet.Snippet;

public class TestSnippetProvider {

  Snippet getSuitableSnippets(CharSequence query) {
		/*
		List<Passage> passageList =  Stream.of("Предложение 1", "Предложение 2", "Предложение 3\n с продолжением.")
			.map(s -> new Passage(s, new BaseQuery(query)))
			.collect(Collectors.toList());
		return new ClusteredSnippet("Заголовок", new Cluster(passageList));
		*/
    return new Snippet() {

      @Override
      public CharSequence getTitle() {
        // TODO Auto-generated method stub
        return "Заголовок 1";
      }

      @Override
      public CharSequence getContent() {
        // TODO Auto-generated method stub
        return "Content for query " + "\"" + query + "\"";
      }

      @Override
      public List<Segment> getSelection() {
        // TODO Auto-generated method stub
        return Arrays.asList(
            new Segment(1, 10));
      }

    };
  }
}
