package components.statistics;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import components.Constants;
import components.crawler.document.CrawlerDocument;

public class Stats {
	
	private Map<String, Long> numberOfDocumentsWithWord = new TreeMap<>();
	private Map<String, Long> numberOfWordOccurences = new TreeMap<>();

	@JsonSerialize(keyUsing = PairSerializer.class)
	@JsonDeserialize(keyUsing = PairDeserializer.class)
	private Map<Pair, Long> frequencyInDocument = new TreeMap<>();
	
	public static Stats readStatsFromFile(String filename) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(new File(filename), Stats.class);
	}
	
	public void writeToFile(String filename) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File(filename), this);
	}

	private static <K, V> void inc(Map<K, Long> m, K key) {
		Long value = m.get(key);
		if (value == null) {
			value = 0l;
		}
		value = value + 1;
		m.put(key, value);
	}
	
	public void acceptDocument(CrawlerDocument doc) {
		Set<String> wordSet = new TreeSet<>();
		for (String w : doc.getContent().toString().split(Constants.getBigramsRegexp())) {
			if (!w.isEmpty()) {
				wordSet.add(w);
				inc(numberOfWordOccurences, w);
				inc(frequencyInDocument, new Pair(w, doc.getID()));
			}
		}
		
		for (String w : wordSet) {
			inc(numberOfDocumentsWithWord, w);
		}
	}
	
	public Long getTermFrequencyInDocument(String term, Long document) {
		return frequencyInDocument.get(new Pair(term, document));
	}
	
	// auto generated getters and setters for serialization
	public Map<String, Long> getNumberOfDocumentsWithWord() {
		return numberOfDocumentsWithWord;
	}

	public void setNumberOfDocumentsWithWord(Map<String, Long> numberOfDocumentsWithWord) {
		this.numberOfDocumentsWithWord = numberOfDocumentsWithWord;
	}

	public Map<String, Long> getNumberOfWordOccurences() {
		return numberOfWordOccurences;
	}

	public void setNumberOfWordOccurences(Map<String, Long> numberOfWordOccurences) {
		this.numberOfWordOccurences = numberOfWordOccurences;
	}

	public Map<Pair, Long> getFrequencyInDocument() {
		return frequencyInDocument;
	}

	public void setFrequencyInDocument(Map<Pair, Long> frequencyInDocument) {
		this.frequencyInDocument = frequencyInDocument;
	}
}
