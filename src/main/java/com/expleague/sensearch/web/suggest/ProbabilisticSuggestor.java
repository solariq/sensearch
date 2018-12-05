package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.index.Index;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ProbabilisticSuggestor implements Suggestor {

	private Map<Term, Double> unigramCoeff;
	private Map<List<Term>, Double> multigramFreqNorm;
	private Map<List<Term>, Double> phraseProb = new HashMap<>();
	
	private Index index;

	public ProbabilisticSuggestor(Crawler crawl, Index index, Config config) throws JsonParseException, JsonMappingException, IOException {
		this.index = index;
		
		SuggestStatisticsProvider provider = new SuggestStatisticsProvider(crawl, index, config);
		unigramCoeff = provider.getUnigramCoeff();
		multigramFreqNorm = provider.getMultigramFreqNorm();
	}

	@Override
	public List<String> getSuggestions(String searchString) {
		System.out.println("suggest requested: " + searchString);
		List<String> res = null;
		try {
			res = getSuggestions(index.parse(searchString).collect(Collectors.toList()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("returning suggest");
		return res;
	}

	private double getPpqt(List<Term> phrase, String qt) {
		double res = 0;
		for (Term c : phrase) {
			if (c.text().toString().startsWith(qt)) {
				res += unigramCoeff.get(c) * multigramFreqNorm.get(phrase);
			}
		}
		return res;
	}

	public List<String> getSuggestions(List<Term> terms) {

		String qt = terms.get(terms.size() - 1).text().toString();
		
		phraseProb.clear();
		
		multigramFreqNorm.keySet().stream()
		.forEach(p -> phraseProb.put(p, getPpqt(p, qt)));
		
		String qc = terms.subList(0, terms.size() - 1)
				.stream()
				.map(t -> t.text())
				.collect(Collectors.joining(" "));
		
		return phraseProb.entrySet().stream()
				.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
				.limit(10)
				.map(e -> qc + " " + e.getKey()
					.stream()
					.map(t -> t.text())
					.collect(Collectors.joining(" ")))
				.collect(Collectors.toList());
	}

}
