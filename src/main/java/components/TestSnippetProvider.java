package components;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import components.query.BaseQuery;
import components.snippeter.snippet.Cluster;
import components.snippeter.snippet.ClusteredSnippet;
import components.snippeter.snippet.Passage;
import components.snippeter.snippet.Snippet;

public class TestSnippetProvider {
	Snippet getSuitableSnippets(CharSequence query) {
		List<Passage> passageList =  Stream.of("Предложение 1", "Предложение 2", "Предложение 3\n с продолжением.")
			.map(s -> new Passage(s, new BaseQuery(query)))
			.collect(Collectors.toList());
		return new ClusteredSnippet("Заголовок", new Cluster(passageList));
	}
}
