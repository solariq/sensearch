package com.expleague.sensearch.web.suggest.hh_phrases;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.PropertyConfigurator;

class Phrase {

	final Term[] content;
	double hhFeature;
	double coocSum;

	public Phrase(Term[] content,
			double hhFeature,
			double coocSum) {
		this.content = content;
		this.hhFeature = hhFeature;
		this.coocSum = coocSum;
	}
	
	public Phrase(Term[] content) {
		this.content = content;
	}
}

public class HHPhrases {

	public double idf(Term t) {
		int df = t.documentFreq();
		return Math.pow(1 / (df + 0.5), 0.5);
	}

	private final Map<List<Term>, Double> map = new HashMap<>();
	private Term[] terms;

	final static double z = 0.75;
	
	public boolean termToSkip(Term t) {
		/*
		return t.partOfSpeech() == PartOfSpeech.PR
				|| t.partOfSpeech() == PartOfSpeech.CONJ
				|| t.partOfSpeech() == PartOfSpeech.PART;
		*/
		return false;
	}
	
	public void countSums(Map<List<Term>, Double> map, int windowSize) {
		Set<Term> nearestForPosition = new HashSet<>();
		for (int i = 0; i < terms.length; i++) {
			Term t = terms[i];
			if (termToSkip(t)) {
				continue;
			}
			nearestForPosition.clear();
			for (int j = i - 1; j >= 0 && i - j < windowSize; j--) {
				if (nearestForPosition.add(terms[j])) {
					Term t1 = terms[j];
					if (termToSkip(t1))
						continue;
					List<Term> key = Arrays.asList(terms[i], terms[j]);
					map.putIfAbsent(key, 0.0);
					double oldval = map.get(key);
					oldval += 1 / Math.pow(Math.abs(i - j), z) * 
							(t.equals(t1) ? 0.25 : 1.0);
					map.put(key, oldval);
				}
			}

			nearestForPosition.clear();
			for (int j = i + 1; j < terms.length && j - i < windowSize; j++) {
				if (nearestForPosition.add(terms[j])) {
					Term t1 = terms[j];
					if (termToSkip(t1))
						continue;
					List<Term> key = Arrays.asList(terms[i], terms[j]);
					map.putIfAbsent(key, 0.0);
					double oldval = map.get(key);
					oldval += 1 / Math.pow(Math.abs(i - j), z) * 
							(t.equals(t1) ? 0.25 : 1.0);
					map.put(key, oldval);
				}
			}
		}
	}

	public void qualifiedPhrase(Phrase p) {
		Term[] phrase = p.content;
		double res = 0, cooc = 0;
		for (Term t : phrase) {
			for (Term t1 : phrase) {
				List<Term> key = Arrays.asList(t, t1);
				res += map.containsKey(key) ? map.get(key) * idf(t) * idf(t1): 0;
				cooc += map.containsKey(key) ? map.get(key) : 0;
			}
		}
		p.coocSum = cooc;
		p.hhFeature = Math.log(1 + res);
	}

	void incCounter(int[] cnt, int windowSize, int maxVal) {
		for (int j = cnt.length - 1; j >= 0; j--) {
			if (cnt[j] - cnt[0] < windowSize + j - cnt.length && cnt[j] < maxVal + j - cnt.length + 1) {
				cnt[j]++;
				for (int i = j + 1; i < cnt.length; i++) {
					cnt[i] = cnt[j] + i - j;
				}
				return;
			}
		}
	}

	public Set<Phrase> bestNGrams(int n, int windowSize, int limit) {
		int[] idxs = new int[n];
		for (int i = 0; i < n; i++)
			idxs[i] = i;

		Set<Phrase> res = new TreeSet<>((p1, p2) -> Double.compare(p1.hhFeature, p2.hhFeature));
		while (idxs[0] < terms.length - n) {
			Term[] val = new Term[n];
			for (int i = 0; i < n; i++) {
				val[i] = terms[idxs[i]];
			}
			incCounter(idxs, windowSize, terms.length - 1);
			Phrase p = new Phrase(val);
			qualifiedPhrase(p);
			res.add(p);
			res.removeIf(d -> res.size() > limit);
		}
		return res;
	}

	Index index;

	public HHPhrases() throws IOException {
		Properties logProperties = new Properties();
		logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
		PropertyConfigurator.configure(logProperties);

		Config config =
				new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
		Injector injector = Guice.createInjector(new AppModule(config));
		index = injector.getInstance(Index.class);
	}

	public void processPhrases(String pageName) {

		Page p = null;
		try {
			p = index.page(new URI(pageName));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CharSequence content = p.content(SegmentType.BODY);
		System.out.println("Content length " + content.length());
		terms = index.parse(content)
				//.map(t -> t.lemma())
				.toArray(Term[]::new);
		System.out.println("terms length: " + terms.length);
		countSums(map, 20);
		bestNGrams(2, 10, 20).forEach(phrase -> {
			for (Term t : phrase.content) {
				System.out.print(t.text() + " ");
			}
			System.out.format("%.3f %.3f\n", phrase.hhFeature, phrase.coocSum);
		});
	}

	public static void main(String[] args) throws IOException {
		List<String> pageURLs = Arrays.asList(
				"https://ru.wikipedia.org/wiki/Москва",
				"https://ru.wikipedia.org/wiki/Миронов,_Андрей_Александрович");
		HHPhrases phrases = new HHPhrases();
		pageURLs.forEach(u -> {
			System.out.println(u);
			phrases.processPhrases(u);
		});
	}
}
