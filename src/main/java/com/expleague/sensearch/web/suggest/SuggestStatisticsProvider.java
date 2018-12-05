package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.index.Index;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SuggestStatisticsProvider {
	private final int maxNgramsOrder = 3;
	
	private Path unigramsStorage;
	private Path multigramsStorage;
	
	private int ndocs;
	
	private Index index;
	private Crawler crawler;
	private ObjectMapper mapper = new ObjectMapper();

	private Map<List<String>, Integer> multigramFreq = new HashMap<>();
	private Map<String, Integer> unigramFreq = new HashMap<>();
	private Map<String, Integer> unigramDF = new HashMap<>();
	private Map<String, Double> sumFreqNorm = new HashMap<>();
	 
	private double[] avgOrderFreq = new double[maxNgramsOrder];
	
	private Map<String, Double> unigramCoeff = new HashMap<>();
	private Map<List<String>, Double> multigramFreqNorm = new HashMap<>();
	
	private <K> void addToMap(Map<K, Integer> m, K key, int inc) {
		Integer oldVal = m.get(key);
		int oVal = oldVal == null ? 0 : oldVal;
		m.put(key, oVal + inc);
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
		computeTargetMaps();
	}
	
	private boolean tryLoad() throws JsonParseException, JsonMappingException, IOException {
		if (!Files.exists(unigramsStorage) || !Files.exists(multigramsStorage)) {
			return false;
		}
		
		unigramCoeff = mapper.readValue(unigramsStorage.toFile(), unigramCoeff.getClass());
		multigramFreqNorm = mapper.readValue(multigramsStorage.toFile(), multigramFreqNorm.getClass());
		
		return true;
	}
	
	private void saveTargets() throws JsonGenerationException, JsonMappingException, IOException {
		mapper.writeValue(unigramsStorage.toFile(), unigramCoeff);
		mapper.writeValue(multigramsStorage.toFile(), multigramFreqNorm);
	}
	
	public SuggestStatisticsProvider(Crawler crawler, Index index, Config config) throws JsonParseException, JsonMappingException, IOException {
		
		unigramsStorage = config.getTemporaryIndex().resolve("/Suggest/unigram_coeff");
		multigramsStorage = config.getTemporaryIndex().resolve("/Suggest/multigram_coeff");

		if (tryLoad()) {
			System.out.println("Suggest information loaded");
			return;
		}
		
		this.crawler = crawler;
		this.index = index;
		
		try {
			makeComputations();
		} catch (Exception e) {
			System.out.println("suggestor: exception while computing occured. " + e.getMessage());
		}
		
		saveTargets();
		System.out.println("Suggest information computed and saved");
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

	private void computeTargetMaps() {
		multigramFreq.keySet().stream()
		.forEach(mtgr -> multigramFreqNorm.put(mtgr, freqNorm(mtgr)));
		
		unigramFreq.keySet().stream()
		.forEach(ung -> unigramCoeff.put(ung,
				unigramFreq.get(ung) * Math.log(ndocs / unigramDF.get(ung)) / sumFreqNorm.get(ung)));
	}
	
	public Map<List<String>, Double> getMultigramFreqNorm() {
		return multigramFreqNorm;
	}
	
	public Map<String, Double> getUnigramCoeff() {
		return unigramCoeff;
	}
}
