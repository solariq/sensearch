package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProbabilisticSuggestor implements Suggestor {

	private Map<Term, Double> unigramCoeff;
	private Map<Term[], Double> multigramFreqNorm;
	private Map<Term, int[]> invertedIndex = new HashMap<>();

	private Map<Term[], Double> phraseProb = new HashMap<>();

	private Index index;
	
	@Inject
	public ProbabilisticSuggestor(Index index) {
		this.index = index;

		SuggestInformationLoader provider = index.getSuggestInformation();
		unigramCoeff = provider.unigramCoeff;
		multigramFreqNorm = provider.multigramFreqNorm;
		invertedIndex = provider.invertedIndex;

	}

	@Override
	public List<String> getSuggestions(String searchString) {
		System.out.println("suggest requested: " + searchString);
		List<String> res = null;
		try {
			res = getSuggestions(index.parse(searchString.toLowerCase())
					.collect(Collectors.toList()));

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("returning suggest");
		return res;
	}

	private double getPpQt(Term[] phrase, String qt) {
		double res = 0;
		for (Term c : phrase) {
			if (c.text().toString().startsWith(qt)) {
				res += unigramCoeff.get(c) * multigramFreqNorm.get(phrase);
			}
		}
		return res;
	}

	private List<Integer> getDocumentList(Term t) {
		if (!invertedIndex.containsKey(t)) {
			return Arrays.asList();
		}
		List<Integer> res = Arrays.stream(invertedIndex.get(t))
				.boxed()
				.collect(Collectors.toList());

		if (res == null) {
			return new ArrayList<>();
		}
		return res;
	}

	private List<Integer> getDocsSetsIntersection(List<Integer> init, Term[] terms) {
		for (Term t : terms) {
			init.retainAll(getDocumentList(t));
		}
		return init;
	}

	private List<Integer> getDocsSetsIntersection(Term[] terms) {
		if (terms.length == 0) {
			return new ArrayList<>();
		}

		return getDocsSetsIntersection(getDocumentList(terms[0]), terms);
	}

	private double getPQcp(List<Integer> docsForQc, Term[] phrase) {
		List<Integer> init = new ArrayList<>(docsForQc);
		return getDocsSetsIntersection(init, phrase).size() + 0.5;
	}

	private List<String> getSuggestions(List<Term> terms) {
		
		if (terms.isEmpty()) {
			return Arrays.asList();
		}
		
		String qt = terms.get(terms.size() - 1).text().toString();
		List<Term> qc = terms.subList(0, terms.size() - 1);

		List<Integer> qcDocs = getDocsSetsIntersection(qc.stream().toArray(Term[]::new));
		//List<Integer> qcDocs = Arrays.asList();
		
		phraseProb.clear();

		for (Term[] p : multigramFreqNorm.keySet()) {
			phraseProb.put(p, getPpQt(p, qt) * getPQcp(qcDocs, p));
		}

		String qcText = qc
				.stream()
				.map(t -> t.text())
				.collect(Collectors.joining(" "));

		return phraseProb.entrySet().stream()
				.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
				.limit(10)
				.map(e -> qcText + " " + Arrays.stream(e.getKey())
				.map(t -> t.text())
				.collect(Collectors.joining(" ")))
				.collect(Collectors.toList());
	}

}
