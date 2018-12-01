package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.index.Index;

public class ProbabilisticSuggestor implements Suggestor {

	private final int maxNgramsOrder = 3;

	private int ndocs;
	
	private Index index;
	private Crawler crawler;

	private Map<List<String>, Integer> multigramFreq;
	private Map<List<String>, Double> phraseProb;
	private Map<String, Integer> unigramFreq;
	private Map<String, Integer> unigramDF;
	private Map<String, Double> sumFreqNorm;
	private double[] avgOrderFreq;

	private <K> void addToMap(Map<K, Integer> m, K key, int inc) {
		Integer oldVal = m.get(key);
		int oVal = oldVal == null ? 0 : oldVal;
		m.put(key, oVal + inc);
	}

	private void init() {
		multigramFreq = new HashMap<>();
		unigramFreq = new HashMap<>();
		unigramDF = new HashMap<>();
		sumFreqNorm = new HashMap<>();

		phraseProb = new HashMap<>();

		avgOrderFreq = new double[maxNgramsOrder];
		
		ndocs = 0;
	}

	private void makeComputations() throws IOException, XMLStreamException {
		
		List<String> titles = crawler
				.makeStream()
				.peek(t -> ndocs++)
				.map(p -> p.title().toString())
				.collect(Collectors.toList());
		System.out.println("getSuggestions: titles recieved " + ndocs);
		
		
		computeUnigrams(titles);
		computeMultigrams(titles);
		computeAvgOrderFreq();
		computeFreqNorm();
	}

	public ProbabilisticSuggestor(Crawler crawl, Index index) {
		crawler = crawl;
		this.index = index;
		
		init();
		
		try {
			makeComputations();
		} catch (Exception e) {
			System.out.println("suggestor: exception while computing occured. " + e.getMessage());
		}
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

	private void computeUnigrams(List<String> texts) {
		for (String t : texts) {
			index.parse(t)
			.map(trm -> trm.text().toString())
			.filter(s -> !s.isEmpty())
			.peek(s -> {
				addToMap(unigramFreq, s, 1);
				addToMap(multigramFreq, Arrays.asList(s), 1);
				sumFreqNorm.put(s, 0.0);
			})
			.distinct()
			.forEach(s -> addToMap(unigramDF, s, 1));
		}
	}

	private List<List<String>> getNgrams(String sentence, int order) {
		List<String> unigrams = index.parse(sentence).map(t -> t.text().toString())
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());

		List<List<String>> result = new ArrayList<>();

		for (int i = 0; i < unigrams.size() - order + 1; i++) {
			result.add(unigrams.subList(i, i + order));
		}

		return result;
	}

	private void computeMultigrams(List<String> texts) {
		for (int i = 2; i <= maxNgramsOrder; i++) {
			for (String t : texts) {
				getNgrams(t, i).stream()
				.forEach(l -> addToMap(multigramFreq, l, 1));
			}
		}
	}

	private void computeAvgOrderFreq() {
		double[] countOfOrder = new double[maxNgramsOrder];

		multigramFreq.entrySet().stream()
		.forEach(ent -> {
			int idx = ent.getKey().size() - 1;
			countOfOrder[idx]++;
			avgOrderFreq[idx] += ent.getValue();
		});

		for (int i = 1; i < maxNgramsOrder; i++) {
			avgOrderFreq[i] /= countOfOrder[i];
		}
	}

	private double freqNorm(List<String> phrase) {
		return multigramFreq.get(phrase) / Math.log(avgOrderFreq[phrase.size() - 1]);
	}

	private void computeFreqNorm() {
		for (List<String> l : multigramFreq.keySet()) {
			double fNorm = freqNorm(l);
			for (String s : l) {
				sumFreqNorm.put(s, sumFreqNorm.get(s) + fNorm);
			}
		}
	}

	private double getPpqt(List<String> phrase, String qt, double ndocs) {
		double res = 0;
		for (String c : phrase) {
			if (c.startsWith(qt)) {
				res += unigramFreq.get(c) * Math.log(ndocs / unigramDF.get(c))
						* freqNorm(phrase) / sumFreqNorm.get(c);
			}
		}
		return res;
	}

	public List<String> getSuggestions(List<Term> terms) {

		String qt = terms.get(terms.size() - 1).text().toString();
		
		phraseProb.clear();
		
		multigramFreq.keySet().stream()
		.forEach(p -> phraseProb.put(p, getPpqt(p, qt, ndocs)));
		
		String qc = terms.subList(0, terms.size() - 1)
				.stream()
				.map(t -> t.text())
				.collect(Collectors.joining(" "));
		
		return phraseProb.entrySet().stream()
				.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
				.limit(10)
				.map(e -> qc + " " + e.getKey()
					.stream()
					.collect(Collectors.joining(" ")))
				.collect(Collectors.toList());
	}

}
