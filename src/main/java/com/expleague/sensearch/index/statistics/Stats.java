package com.expleague.sensearch.index.statistics;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.expleague.sensearch.index.IndexedPage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Stats {
	
	private Map<String, Integer> numberOfDocumentsWithWord = new HashMap<>();
	private Map<String, Integer> numberOfWordOccurences = new HashMap<>();
	private Map<Long, Integer> documentLength = new HashMap<>();
	
	@JsonSerialize(keyUsing = PairSerializer.class)
	@JsonDeserialize(keyUsing = PairDeserializer.class)
	private Map<Pair, Integer> frequencyInDocument = new HashMap<>();
	
	private long totalNumberOfDocuments;
	private long totalLength;
	
	//BM25 coefficients
	final double k1 = 2.0, b = 0.75;
	
	public double getBM25(Long documentID, String query) {
		double res = 0;
		for (String w : query.split("[^a-zA-Zа-яА-ЯЁё]+")) {
			Integer ni = numberOfDocumentsWithWord.get(w);
			if (ni == null)
				ni = 0;
			
			Integer fi = frequencyInDocument.get(new Pair(w, documentID));
			if (fi == null)
				fi = 0;
			
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

	private static <K, V> void inc(Map<K, Integer> m, K key) {
		Integer value = m.get(key);
		if (value == null) {
			value = 0;
		}
		value = value + 1;
		m.put(key, value);
	}
	
	public void acceptDocument(IndexedPage doc) {
		Set<String> wordSet = new HashSet<>();
		totalNumberOfDocuments++;
		
		String[] words = doc.text().toString().split("[^a-zA-Zа-яА-ЯЁё]+");
		int docLength = 0;
		
		documentLength.put(doc.id(), Integer.valueOf(words.length));
		for (String w : words) {
			if (!w.isEmpty()) {
				docLength++;
				wordSet.add(w);
				inc(numberOfWordOccurences, w);
				inc(frequencyInDocument, new Pair(w, doc.id()));
			}
		}
		
		documentLength.put(doc.id(), docLength);
		totalLength += docLength;
		
		for (String w : wordSet) {
			inc(numberOfDocumentsWithWord, w);
		}
	}
	
	public Integer getTermFrequencyInDocument(String term, Long document) {
		return frequencyInDocument.get(new Pair(term, document));
	}
	
	@JsonIgnore
	public int getVocabularySize() {
		return numberOfWordOccurences.size();
	}
	
	@JsonIgnore
	public double getAverageDocumentLength() {
		return totalLength / totalNumberOfDocuments;
	}
	
	// auto generated getters and setters for serialization
	public Map<String, Integer> getNumberOfDocumentsWithWord() {
		return numberOfDocumentsWithWord;
	}

	public void setNumberOfDocumentsWithWord(Map<String, Integer> numberOfDocumentsWithWord) {
		this.numberOfDocumentsWithWord = numberOfDocumentsWithWord;
	}

	public Map<String, Integer> getNumberOfWordOccurences() {
		return numberOfWordOccurences;
	}

	public void setNumberOfWordOccurences(Map<String, Integer> numberOfWordOccurences) {
		this.numberOfWordOccurences = numberOfWordOccurences;
	}

	public Map<Pair, Integer> getFrequencyInDocument() {
		return frequencyInDocument;
	}

	public void setFrequencyInDocument(Map<Pair, Integer> frequencyInDocument) {
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
	
	public Map<Long, Integer> getDocumentLength() {
		return documentLength;
	}

	public void setDocumentLength(Map<Long, Integer> documentLength) {
		this.documentLength = documentLength;
	}
}
