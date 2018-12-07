package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.util.ArrayList;
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
	private Map<Term, List<Integer>> invertedIndex = new HashMap<>();
	
	private Map<List<Term>, Double> phraseProb = new HashMap<>();
	
	private Index index;

	public ProbabilisticSuggestor(Crawler crawl, Index index, Config config) throws JsonParseException, JsonMappingException, IOException {
		this.index = index;
		
		SuggestStatisticsProvider provider = new SuggestStatisticsProvider(crawl, index, config);
		unigramCoeff = provider.getUnigramCoeff();
		multigramFreqNorm = provider.getMultigramFreqNorm();
		invertedIndex = provider.getInvertedIndex();
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

	private double getPpQt(List<Term> phrase, String qt) {
		double res = 0;
		for (Term c : phrase) {
			if (c.text().toString().startsWith(qt)) {
				res += unigramCoeff.get(c) * multigramFreqNorm.get(phrase);
			}
		}
		return res;
	}
	
	private List<Integer> getDocumentList(Term t) {
		List<Integer> res = invertedIndex.get(t);
		if (res == null) {
			return new ArrayList<>();
		}
		return res;
	}
	
	private List<Integer> getDocsSetsIntersection(List<Integer> init, List<Term> terms) {
		for (Term t : terms) {
			init.retainAll(getDocumentList(t));
		}
		return init;
	}
	
	private List<Integer> getDocsSetsIntersection(List<Term> terms) {
		if (terms.isEmpty()) {
			return new ArrayList<>();
		}
		
		return getDocsSetsIntersection(getDocumentList(terms.get(0)), terms);
	}
	
	private double getPQcp(List<Integer> docsForQc, List<Term> phrase) {
		List<Integer> init = new ArrayList<>(docsForQc);
		return getDocsSetsIntersection(init, phrase).size() + 0.5;
	}
	
	public List<String> getSuggestions(List<Term> terms) {

		String qt = terms.get(terms.size() - 1).text().toString();
		List<Term> qc = terms.subList(0, terms.size() - 1);
		
		List<Integer> qcDocs = getDocsSetsIntersection(qc);
		
		phraseProb.clear();
		
		multigramFreqNorm.keySet().stream()
		.forEach(p -> phraseProb.put(p, getPpQt(p, qt) * getPQcp(qcDocs, p)));
		
		String qcText = qc
				.stream()
				.map(t -> t.text())
				.collect(Collectors.joining(" "));
		
		return phraseProb.entrySet().stream()
				.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
				.limit(10)
				.map(e -> qcText + " " + e.getKey()
					.stream()
					.map(t -> t.text())
					.collect(Collectors.joining(" ")))
				.collect(Collectors.toList());
	}

}
