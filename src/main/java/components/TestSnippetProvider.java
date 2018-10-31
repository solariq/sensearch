package components;

import components.snippeter.Segment;
import components.snippeter.Snippet;
import java.util.Arrays;
import java.util.List;

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
