package com.expleague.sensearch.index.statistics;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Stats {
	
	private Map<String, Long> numberOfDocumentsWithWord = new TreeMap<>();
	private Map<String, Long> numberOfWordOccurences = new TreeMap<>();
	private Map<Long, Long> documentLength = new TreeMap<>();
	
	@JsonSerialize(keyUsing = PairSerializer.class)
	@JsonDeserialize(keyUsing = PairDeserializer.class)
	private Map<Pair, Long> frequencyInDocument = new TreeMap<>();
	
	private long totalNumberOfDocuments;
	private long totalLength;
	
	//BM25 coefficients
	final double k1 = 2.0, b = 0.75;
	
	public double getBM25(Long documentID, String query) {
		double res = 0;
		for (String w : query.split("[^a-zA-Zа-яА-ЯЁё]+")) {
			Long ni = numberOfDocumentsWithWord.get(w);
			if (ni == null)
				ni = 0l;
			
			Long fi = frequencyInDocument.get(new Pair(w, documentID));
			if (fi == null)
				fi = 0l;
			
			res += Math.log((totalNumberOfDocuments - ni + 0.5) / (ni + 0.5))
					* fi * (k1 + 1) / (fi + k1 * (1 - b +
							b * documentLength.get(documentID) / getAverageDocumentLength()));
		}
		
		return res;
	}
	
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
		totalNumberOfDocuments++;
		
		String[] words = doc.getContent().toString().split("[^a-zA-Zа-яА-ЯЁё]+");
		long docLength = 0;
		
		documentLength.put(doc.getID(), Long.valueOf(words.length));
		for (String w : words) {
			if (!w.isEmpty()) {
				docLength++;
				wordSet.add(w);
				inc(numberOfWordOccurences, w);
				inc(frequencyInDocument, new Pair(w, doc.getID()));
			}
		}
		
		documentLength.put(doc.getID(), docLength);
		totalLength += docLength;
		
		for (String w : wordSet) {
			inc(numberOfDocumentsWithWord, w);
		}
	}
	
	public Long getTermFrequencyInDocument(String term, Long document) {
		return frequencyInDocument.get(new Pair(term, document));
	}
	
	@JsonIgnore
	public int getVocabularySize() {
		return numberOfWordOccurences.size();
	}
	
	@JsonIgnore
	public long getAverageDocumentLength() {
		return totalLength / totalNumberOfDocuments;
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

	public long getTotalNumberOfDocuments() {
		return totalNumberOfDocuments;
	}

	public void setTotalNumberOfDocuments(long totalNumberOfDocuments) {
		this.totalNumberOfDocuments = totalNumberOfDocuments;
	}

	public long getTotalLength() {
		return totalLength;
	}

	public void setTotalLength(long totalLength) {
		this.totalLength = totalLength;
	}
	
	public Map<Long, Long> getDocumentLength() {
		return documentLength;
	}

	public void setDocumentLength(Map<Long, Long> documentLength) {
		this.documentLength = documentLength;
	}
}
