package com.expleague.sensearch.web.suggest.hh_phrases;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HHPhrases {
	
	public double idf(Term t) {
		int df = t.documentFreq();
		return Math.log((index.size() - df + 0.5) / (df + 0.5));
	}
	
	public final Map<List<Term>, Double> map = new HashMap<>();
	Term[] terms;
	
	final static double z = 0.75;
	
	public void countSums(Map<List<Term>, Double> map, Term[] terms) {
		Set<Term> nearestForPosition = new HashSet<>();
		for (int i = 0; i < terms.length; i++) {
			Term t = terms[i];
			nearestForPosition.clear();
			for (int j = i - 1; j >= 0; j--) {
				if (nearestForPosition.add(terms[j])) {
					Term t1 = terms[j];
					List<Term> key = Arrays.asList(terms[i], terms[j]);
					map.putIfAbsent(key, 0.0);
					double oldval = map.get(key);
					oldval += idf(t1) / Math.pow(Math.abs(i - j), z) * 
							(t.equals(t1) ? 0.25 : 1.0);
					map.put(key, oldval);
				}
			}
			
			nearestForPosition.clear();
			for (int j = i + 1; j < terms.length; j++) {
				if (nearestForPosition.add(terms[j])) {
					Term t1 = terms[j];
					List<Term> key = Arrays.asList(terms[i], terms[j]);
					map.putIfAbsent(key, 0.0);
					double oldval = map.get(key);
					oldval += idf(t1) / Math.pow(Math.abs(i - j), z) * 
							(t.equals(t1) ? 0.25 : 1.0);
					map.put(key, oldval);
				}
			}
		}
	}
	
	public double qualifiedPhrase(Term[] phrase) {
		double res = 0;
		for (Term t : phrase) {
			for (Term t1 : phrase) {
				List<Term> key = Arrays.asList(t, t1);
				res += map.containsKey(key) ? map.get(key) * idf(t) : 0;
			}
		}
		return Math.log(1 + res);
	}
	
	int incCounter(int[] cnt, int base) {
		int add = 1;
		for (int i = 0; i < cnt.length; i++) {
			cnt[i] += add;
			add = 0;
			if (cnt[i] == base) {
				cnt[i] = 0;
				add = 1;
			}
		}
		return add;
	}
	
	public Map<Double, Term[]> bestNGrams(int n, int limit) {
		int[] idxs = new int[n];
		Map<Double, Term[]> res = new TreeMap<>();
		while (incCounter(idxs, terms.length) == 0) {
			Term[] val = new Term[n];
			for (int i = 0; i < n; i++) {
				val[i] = terms[idxs[i]];
			}
			res.put(qualifiedPhrase(val), val);
			res.keySet().removeIf(d -> res.size() > limit);
		}
		return res;
	}
	
	Index index;
	public void processPhrases() throws JsonParseException, JsonMappingException, IOException, URISyntaxException {
		ObjectMapper objectMapper = new ObjectMapper();
	    Config config = objectMapper.readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);

		index = new PlainIndex(config);
    	Page p = index.page(new URI("https://ru.wikipedia.org/wiki/Москва"));
    	CharSequence content = p.content();
    	System.out.println("Content length " + content.length());
		terms = index.parse(content)
				//.map(t -> t.lemma())
				.toArray(Term[]::new);
		
		countSums(map, terms);
		bestNGrams(2, 20).forEach((d, tr) -> {
			for (Term t : tr) {
				System.out.print(t.text() + " ");
			}
			System.out.println(d);
		});
	}
	
	public static void main(String[] args) throws IOException, URISyntaxException {
		HHPhrases phrases = new HHPhrases();
		phrases.processPhrases();
	}
}
