package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongObjectMap;
import java.util.HashMap;
import java.util.Map;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class SuggestInformationLoader {
	Map<Term, Double> unigramCoeff = new HashMap<>();
	Map<Term[], Double> multigramFreqNorm = new HashMap<>();
	Map<Term, int[]> invertedIndex = new HashMap<>();

	private final DB unigramCoeffDB;
	private final DB multigramFreqNormDB;

	private final TLongObjectMap<Term> idToTerm;

	public SuggestInformationLoader(DB unigramCoeffDB, DB multigramDB,
			TLongObjectMap<Term> idToTerm) {
		this.unigramCoeffDB = unigramCoeffDB;
		this.multigramFreqNormDB = multigramDB;

		this.idToTerm = idToTerm;

		tryLoad();
	}

	private void tryLoad() {
		{
			DBIterator iter = unigramCoeffDB.iterator();
			iter.seekToFirst();
			iter.forEachRemaining(item -> unigramCoeff.put(
					idToTerm.get(Longs.fromByteArray(item.getKey())),
					Double.longBitsToDouble(Longs.fromByteArray(item.getValue()))
			));
		}

		{
			DBIterator iter = multigramFreqNormDB.iterator();
			iter.seekToFirst();
			iter.forEachRemaining(item -> {
				Term[] terms = null;
				try {
					terms = IndexUnits.TermList.parseFrom(item.getKey())
							.getTermListList().stream()
							.map(idToTerm::get)
							.toArray(Term[]::new);
				} catch (InvalidProtocolBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				multigramFreqNorm.put(
						terms,
						Double.longBitsToDouble(Longs.fromByteArray(item.getValue()))
						);
			});
		}

	}
}
