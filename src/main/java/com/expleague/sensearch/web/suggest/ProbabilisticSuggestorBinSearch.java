package com.expleague.sensearch.web.suggest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ProbabilisticSuggestorBinSearch implements Suggestor {

	private final Map<Term, Double> unigramCoeff;
	private final Map<Term[], Double> multigramFreqNorm;
	private final Map<Term[], Double> phraseProb = new HashMap<>();

	private final ArrayList<Term> sortedUnigrams = new ArrayList<>();
	private final ArrayList<List<Term[]>> parentMultigrams = new ArrayList<>();

	private PlainIndex index;

	@Inject
	public ProbabilisticSuggestorBinSearch(PlainIndex index) {
		this.index = index;

		SuggestInformationLoader provider = index.getSuggestInformation();
		unigramCoeff = provider.unigramCoeff;
		multigramFreqNorm = provider.multigramFreqNorm;

		final TreeMap<Term, List<Term[]>> sortedMultigrams = new TreeMap<>((t1, t2) -> t1.text().toString().compareTo(t2.text().toString()));

		multigramFreqNorm.keySet().forEach(mtgr -> {
			for (Term t : mtgr) {
				sortedMultigrams.putIfAbsent(t, new ArrayList<>());
				sortedMultigrams.get(t).add(mtgr);
			}
		});

		sortedMultigrams.forEach((ung, mglist) -> {
			sortedUnigrams.add(ung);
			parentMultigrams.add(mglist);
		});

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
		System.out.println("returning suggest: " + searchString);
		return res;
	}

	private double getPpQt(Term[] phrase, String qt) {
		double res = 0;
		for (Term c : phrase) {
			if (c.text().toString().startsWith(qt)) {
				//res += unigramCoeff.get(c) * multigramFreqNorm.get(phrase);
				res++;
			}
		}
		return res;
	}

	private int lowerBound(String qt) {
		int l = 0, r = sortedUnigrams.size();
		while (r - l > 1) {
			int m = (r - l) / 2 + l;
			boolean less;
			String atMTerm = sortedUnigrams.get(m).text().toString();
			if (atMTerm.length() >= qt.length()) {
				String atM = sortedUnigrams.get(m).text().toString().substring(0, qt.length());
				less = qt.compareTo(atM) > 0;
			} else {
				less = qt.compareTo(atMTerm) > 0;
			}
			if (less) {
				l = m;
			} else {
				r = m;
			}
		}
		return r;
	}

	private int upperBound(String qt) {
		int l = 0, r = sortedUnigrams.size();
		while (r - l > 1) {
			int m = (r - l) / 2 + l;
			boolean less;
			String atMTerm = sortedUnigrams.get(m).text().toString();
			if (atMTerm.length() >= qt.length()) {
				String atM = sortedUnigrams.get(m).text().toString().substring(0, qt.length());
				less = qt.compareTo(atM) >= 0;
			} else {
				less = qt.compareTo(atMTerm) >= 0;
			}
			if (less) {
				l = m;
			} else {
				r = m;
			}
		}
		return l;
	}

	private List<String> getSuggestions(List<Term> terms) {

		if (terms.isEmpty()) {
			return Collections.emptyList();
		}

		String qt = terms.get(terms.size() - 1).text().toString();
		List<Term> qc = terms.subList(0, terms.size() - 1);

		Vec queryVec = index.vecByTerms(qc);
		
		int lb = lowerBound(qt), ub = upperBound(qt);
		System.out.println(lb + " "+ ub);
		for (int i = lb; i <= ub; i++) {
			for (Term[] p : parentMultigrams.get(i)) {
				if (qc.size() > 0) {
					double pp = getPpQt(p, qt);
					if (pp > 0)
						phraseProb.put(p, pp * VecTools.cosine(queryVec, index.vecByTerms(Arrays.asList(p))));
				} else {
					phraseProb.put(p, getPpQt(p, qt));
				}
			}
		}

		String qcText = qc
				.stream()
				.map(Term::text)
				.collect(Collectors.joining(" "));

		return phraseProb.entrySet().stream()
				.sorted((e1, e2) -> -Double.compare(e1.getValue(), e2.getValue()))
				.limit(10)
				.map(e -> qcText + " " + Arrays.stream(e.getKey())
				.map(Term::text)
				.collect(Collectors.joining(" ")) + String.format(" %.3f", e.getValue()))
				.collect(Collectors.toList());
	}

}
