package com.expleague.sensearch.miner.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.query.Query;

public class HHFeatureSet extends FeatureSet.Stub<QURLItem> implements TextFeatureSet {

	public final static FeatureMeta HHP = FeatureMeta
			.create("hhp", "Title + text hhp", ValueType.VEC);

	private Query query;
	private Set<Term> queryTerms;
	private Map<Term, TreeSet<Integer>> termPositions = new HashMap<>();
	private Map<Term, Double> idf = new HashMap<>();

	@Override
	public void accept(QURLItem item) {
		final Query query = item.queryCache();
		if (query.equals(this.query))
			return;

		this.query = query;
		queryTerms = new HashSet<>(query.terms());

	}

	@Override
	public void withStats(int pageLen, double avgLen, int titleLen, double avgTitle, int indexLen) {
		termPositions.clear();
		queryTerms.forEach(term -> {
			final int df = term.documentFreq();
			final double idf = df == 0 ? 0 : Math.log((indexLen - df + 0.5) / (df + 0.5));
			this.idf.put(term, idf);
		});
	}

	@Override
	public void withSegment(Segment type, Term t) {
		// TODO Auto-generated method stub
	}

	@Override
	public void withTerm(Term t, int offset) {
		termPositions.putIfAbsent(t, new TreeSet<>());
		termPositions.get(t).add(offset);
	}

	@Override
	public Vec advance() {
		set(HHP, hhProximty());
		return super.advance();
	}

	private final double z = 1.75;
	private double frac(Term term, Integer neighPos, int center) {

		if (neighPos == null) {
			return 0;
		}

		return idf.get(term) / Math.pow(Math.abs(neighPos - center), z);
	}

	private double tc(Term t, int p) {
		double res = 0;

		for (Term term : queryTerms) {
			TreeSet<Integer> positions = termPositions.get(term);
			if (positions == null)
				continue;
			
			res += (frac(term, positions.lower(p), p) +
					frac(term, positions.higher(p), p))
					* (t.equals(term) ? 0.25 : 1);
		}

		return res;
	}

	private double atc(Term t) {
		double res = 0;
		
		if (!termPositions.containsKey(t))
			return res;
		
		for (Integer p : termPositions.get(t)) {
			res += tc(t, p);
		}

		return res;
	}

	private double hhProximty() {
		double sum = 0;
		for (Term t : queryTerms) {
			sum += atc(t) * idf.get(t);
		}

		return Math.log(1 + sum);
	}
}
