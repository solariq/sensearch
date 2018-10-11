package components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import components.query.BaseQuery;
import components.snippeter.snippet.Cluster;
import components.snippeter.snippet.Passage;

public class TestSnippetProvider {
	Cluster getSuitableSnippets(String ignored) {
		List<Passage> passageList =  Stream.of("Предложение 1", "Предложение 2", "Предложение 3\n с продолжением.")
			.map(s -> new Passage(s, new BaseQuery(ignored)))
			.collect(Collectors.toList());
		return new Cluster(passageList);
	}
}
