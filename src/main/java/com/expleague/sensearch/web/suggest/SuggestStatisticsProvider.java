package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.IndexTerm;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import gnu.trove.map.TLongObjectMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class SuggestStatisticsProvider {
	private final int maxNgramsOrder = 3;
	
	private Path unigramsStorage;
	private Path multigramsStorage;
	private Path invIndexStorage;
	
	private int ndocs;
	
	private Index index;
	private Crawler crawler;
	private ObjectMapper mapper = new ObjectMapper();

	private Map<List<Term>, Integer> multigramFreq = new HashMap<>();
	private Map<Term, Integer> unigramFreq = new HashMap<>();
	private Map<Term, Integer> unigramDF = new HashMap<>();
	private Map<Term, Double> sumFreqNorm = new HashMap<>();
	 
	private double[] avgOrderFreq = new double[maxNgramsOrder];
	
	private final TLongObjectMap<Term> idToTerm;
	
	private static final WriteOptions DEFAULT_WRITE_OPTIONS =
		      new WriteOptions().sync(true).snapshot(false);
	private final DB suggestBase;
	
	//Maps, that used in suggestor
	private Map<Term, Double> unigramCoeff = new HashMap<>();
	private Map<List<Term>, Double> multigramFreqNorm = new HashMap<>();
	private Map<Term, List<Integer>> invertedIndex = new HashMap<>();
	
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
		/*
		if (!Files.exists(unigramsStorage)
				|| !Files.exists(multigramsStorage)
				|| !Files.exists(invIndexStorage)) {
			return false;
		}
		
		unigramCoeff = mapper.readValue(unigramsStorage.toFile(), unigramCoeff.getClass());
		multigramFreqNorm = mapper.readValue(multigramsStorage.toFile(), multigramFreqNorm.getClass());
		invertedIndex = mapper.readValue(invIndexStorage.toFile(), invertedIndex.getClass());
		*/
		
		DBIterator iter = suggestBase.iterator();
		iter.seekToFirst();
		
		iter.forEachRemaining(item -> {
			long termId = Longs.fromByteArray(item.getKey());
			unigramCoeff.put(
					idToTerm.get(Longs.fromByteArray(item.getKey())),
					Double.longBitsToDouble(Longs.fromByteArray(item.getValue()))
					);
		});
		return true;
	}
	
	private void saveTargets() throws JsonGenerationException, JsonMappingException, IOException {
		/*
		Files.createDirectories(unigramsStorage.getParent());
		mapper.writeValue(unigramsStorage.toFile(), unigramCoeff);
		mapper.writeValue(multigramsStorage.toFile(), multigramFreqNorm);
		mapper.writeValue(invIndexStorage.toFile(), invertedIndex);
		*/
		
		WriteBatch batch = suggestBase.createWriteBatch();
		unigramCoeff.entrySet().forEach(ent -> {
			batch.put(Longs.toByteArray(((IndexTerm)ent.getKey()).id()),
					Longs.toByteArray(Double.doubleToLongBits(ent.getValue())));
		});
		
		suggestBase.write(batch, DEFAULT_WRITE_OPTIONS);
	}

	@Inject
	public SuggestStatisticsProvider(Crawler crawler, Index index, Config config, TLongObjectMap<Term> idToTerm, DB termStats) throws JsonParseException, JsonMappingException, IOException {
		
		this.idToTerm = idToTerm;
		suggestBase = termStats;
		
		unigramsStorage = config.getTemporaryIndex().resolve("Suggest/unigram_coeff");
		multigramsStorage = config.getTemporaryIndex().resolve("Suggest/multigram_coeff");
		invIndexStorage = config.getTemporaryIndex().resolve("Suggest/inv_index");
		
		System.out.println("try to load suggest...");
		if (tryLoad()) {
			System.out.println("Suggest information loaded");
			return;
		}
		System.out.println("Failed to load suggest. Trying to compute...");
		
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
		int cnt = 0;
		for (String t : texts) {
			int docNum = ++cnt;
			index.parse(t)
			.peek(s -> {
				addToMap(unigramFreq, s, 1);
				//addToMap(multigramFreq, Arrays.asList(s), 1);
				sumFreqNorm.put(s, 0.0);
				if (!invertedIndex.containsKey(s)) {
					invertedIndex.put(s, new ArrayList<>());
				}
				invertedIndex.get(s).add(docNum);
			})
			.distinct()
			.forEach(s -> addToMap(unigramDF, s, 1));
		}
	}

	private List<List<Term>> getNgrams(String sentence, int order) {
		List<Term> unigrams = index.parse(sentence)
				.collect(Collectors.toList());

		List<List<Term>> result = new ArrayList<>();

		for (int i = 0; i < unigrams.size() - order + 1; i++) {
			result.add(unigrams.subList(i, i + order));
		}

		return result;
	}

	private void computeMultigrams(List<String> texts) {
		for (int i = 1; i <= maxNgramsOrder; i++) {
			for (String t : texts) {
				getNgrams(t, i).stream()
				.forEach(l -> {
					addToMap(multigramFreq, l, 1);
					});
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

	private double freqNorm(List<Term> phrase) {
		return multigramFreq.get(phrase) / Math.log(avgOrderFreq[phrase.size() - 1]);
	}

	private void computeFreqNorm() {
		for (List<Term> l : multigramFreq.keySet()) {
			double fNorm = freqNorm(l);
			for (Term s : l) {
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
	
	public Map<List<Term>, Double> getMultigramFreqNorm() {
		return multigramFreqNorm;
	}
	
	public Map<Term, Double> getUnigramCoeff() {
		return unigramCoeff;
	}
	
	public Map<Term, List<Integer>> getInvertedIndex() {
		return invertedIndex;
	}
}
