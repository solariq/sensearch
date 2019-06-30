package com.expleague.sensearch.donkey.randomaccess;

import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

public class ProtoTermIndex extends ProtobufIndex<Integer, Term> {

  private final TIntObjectMap<Term> idToTerm = new TIntObjectHashMap<>();

  public ProtoTermIndex(Path root) {
    super(root, Term.class);
    forEach(term -> idToTerm.put(term.getId(), term));
  }

  @Nullable
  @Override
  public Term value(Integer id) {
    return idToTerm.get(id);
  }

  @Override
  public void put(Integer id, Term value) {
    idToTerm.put(id, value);
    super.put(id, value);
  }

  @Override
  protected byte[] encodeKey(Integer key) {
    return Ints.toByteArray(key);
  }
}
