package com.expleague.sensearch.web.suggest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProbabilisticSuggestor implements Suggestor {

	private final Map<Term, Double> unigramCoeff;
	private final Map<Term[], Double> multigramFreqNorm;
	private final Map<Term[], Double> phraseProb = new HashMap<>();

	private PlainIndex index;

	@Inject
	public ProbabilisticSuggestor(PlainIndex index) {
		this.index = index;

		SuggestInformationLoader provider = index.getSuggestInformation();
		unigramCoeff = provider.unigramCoeff;
		multigramFreqNorm = provider.multigramFreqNorm;

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
	
	private List<String> getSuggestions(List<Term> terms) {

		if (terms.isEmpty()) {
			return Collections.emptyList();
		}

		String qt = terms.get(terms.size() - 1).text().toString();
		List<Term> qc = terms.subList(0, terms.size() - 1);

		phraseProb.clear();

		if (qc.size() > 0) {
			Vec queryVec = index.vecByTerms(qc);
			for (Term[] p : multigramFreqNorm.keySet()) {
				double pp = getPpQt(p, qt);
				if (pp > 0)
					phraseProb.put(p, pp * VecTools.cosine(queryVec, index.vecByTerms(Arrays.asList(p))));
			}
		} else {
			for (Term[] p : multigramFreqNorm.keySet()) {
				phraseProb.put(p, getPpQt(p, qt));
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
