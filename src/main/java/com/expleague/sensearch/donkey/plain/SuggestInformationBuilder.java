package com.expleague.sensearch.donkey.plain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;

public class SuggestInformationBuilder {
	private final int maxNgramsOrder = 3;

	private int ndocs;

	private Map<long[], Integer> multigramFreq = new HashMap<>();
	private Map<Long, Integer> unigramFreq = new HashMap<>();
	private Map<Long, Integer> unigramDF = new HashMap<>();
	private Map<Long, Double> sumFreqNorm = new HashMap<>();

	private double[] avgOrderFreq = new double[maxNgramsOrder];

	private static final WriteOptions DEFAULT_WRITE_OPTIONS =
			new WriteOptions().sync(true).snapshot(false);

	//Maps, that used in suggestor
	private Map<Long, Double> unigramCoeff = new HashMap<>();
	private Map<long[], Double> multigramFreqNorm = new HashMap<>();
	private Map<Long, List<Integer>> invertedIndex = new HashMap<>();

	private final DB unigramCoeffDB;
	private final DB multigramFreqNormDB;
	private final DB invertedIndexDB;

	private <K> void addToMap(Map<K, Integer> m, K key, int inc) {
		Integer oldVal = m.get(key);
		int oVal = oldVal == null ? 0 : oldVal;
		m.put(key, oVal + inc);
	}

	public void build() {
		//computeUnigrams(titles);
		//computeMultigrams(titles);
		computeAvgOrderFreq();
		computeFreqNorm();
		computeTargetMaps();
		saveTargets();
	}

	public void accept(long[] wordIds) {
		computeUnigrams(wordIds);
		computeMultigrams(wordIds);
	}

	private void saveTargets() {
		
		{
			WriteBatch batch = unigramCoeffDB.createWriteBatch();
			unigramCoeff.entrySet().forEach(ent -> {
				batch.put(Longs.toByteArray(ent.getKey()),
						Longs.toByteArray(Double.doubleToLongBits(ent.getValue())));
			});
			unigramCoeffDB.write(batch, DEFAULT_WRITE_OPTIONS);
		}

		{
			WriteBatch batch = multigramFreqNormDB.createWriteBatch();
			multigramFreqNorm.entrySet().forEach(ent -> {

				List<Long> l = Arrays.stream(ent.getKey())
						.boxed()
						.collect(Collectors.toList());


				batch.put(
						IndexUnits.TermList.newBuilder()
						.addAllTermList(l)
						.build()
						.toByteArray(),
						Longs.toByteArray(Double.doubleToLongBits(ent.getValue())));
			});
			multigramFreqNormDB.write(batch, DEFAULT_WRITE_OPTIONS);
		}

		{
			WriteBatch batch = invertedIndexDB.createWriteBatch();
			invertedIndex.entrySet().forEach(ent -> {
				batch.put(
						Longs.toByteArray(ent.getKey()),
						IndexUnits.IntegerList.newBuilder()
						.addAllIntList(ent.getValue())
						.build().
						toByteArray());
			});
			invertedIndexDB.write(batch, DEFAULT_WRITE_OPTIONS);
		}
	}

	@Inject
	public SuggestInformationBuilder(
			DB unigramCoeffDB,
			DB multigramFreqNormDB,
			DB invertedIndexDB)
	{
		this.unigramCoeffDB = unigramCoeffDB;
		this.multigramFreqNormDB = multigramFreqNormDB;
		this.invertedIndexDB = invertedIndexDB;
	}

	private void computeUnigrams(long[] wordIds) {
		int docNum = ndocs++;
		Arrays.stream(wordIds)
		.peek(s -> {
			addToMap(unigramFreq, s, 1);
			sumFreqNorm.put(s, 0.0);
			if (!invertedIndex.containsKey(s)) {
				invertedIndex.put(s, new ArrayList<>());
			}
			invertedIndex.get(s).add(docNum);
		})
		.distinct()
		.forEach(s -> addToMap(unigramDF, s, 1));
	}

	private List<long[]> getNgrams(long[] wordsIds, int order) {

		List<long[]> result = new ArrayList<>();

		for (int i = 0; i < wordsIds.length - order + 1; i++) {
			result.add(Arrays.copyOfRange(wordsIds, i, i + order));
		}

		return result;
	}

	private void computeMultigrams(long[] wordIds) {
		for (int i = 1; i <= maxNgramsOrder; i++) {
			getNgrams(wordIds, i).stream()
			.forEach(l -> {
				addToMap(multigramFreq, l, 1);
			});
		}
	}

	private void computeAvgOrderFreq() {
		double[] countOfOrder = new double[maxNgramsOrder];

		multigramFreq.entrySet().stream()
		.forEach(ent -> {
			int idx = ent.getKey().length - 1;
			countOfOrder[idx]++;
			avgOrderFreq[idx] += ent.getValue();
		});

		for (int i = 1; i < maxNgramsOrder; i++) {
			avgOrderFreq[i] /= countOfOrder[i];
		}
	}

	private double freqNorm(long[] l) {
		return multigramFreq.get(l) / Math.log(avgOrderFreq[l.length - 1]);
	}

	private void computeFreqNorm() {
		for (long[] l : multigramFreq.keySet()) {
			double fNorm = freqNorm(l);
			for (long s : l) {
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
}
