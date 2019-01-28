package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ByteTools {

  private ByteTools() {
  }

  public static byte[] toBytes(List<Vec> vecs) {
    if (vecs.size() == 0) {
      return new byte[0];
    }

    ByteBuffer byteBuf = ByteBuffer.allocate(Double.BYTES * vecs.get(0).length() * vecs.size());
    DoubleBuffer doubleBuf = byteBuf.asDoubleBuffer();
    for (Vec vec : vecs) {
      double[] coords = vec.toArray();
      doubleBuf.put(coords);
    }
    return byteBuf.array();
  }

  public static byte[] toBytes(Vec vec) {
    double[] coords = vec.toArray();
    ByteBuffer byteBuf = ByteBuffer.allocate(Double.BYTES * coords.length);
    DoubleBuffer doubleBuf = byteBuf.asDoubleBuffer();
    doubleBuf.put(coords);
    return byteBuf.array();
  }

  public static Vec toVec(byte[] bytes) {
    DoubleBuffer doubleBuf = ByteBuffer.wrap(bytes).asDoubleBuffer();
    double[] doubles = new double[doubleBuf.remaining()];
    doubleBuf.get(doubles);
    return new ArrayVec(doubles);
  }

  public static byte[] toBytes(long[] longs) {
    ByteBuffer byteBuf = ByteBuffer.allocate(Long.BYTES * longs.length);
    LongBuffer longBuf = byteBuf.asLongBuffer();
    longBuf.put(longs);
    return byteBuf.array();
  }

  public static long[] toLongArray(byte[] bytes) {
    LongBuffer longBuf = ByteBuffer.wrap(bytes).asLongBuffer();
    long[] longs = new long[longBuf.remaining()];
    longBuf.get(longs);
    return longs;
  }

  public static List<Vec> toVecs(byte[] bytes) {
    DoubleBuffer doubleBuf = ByteBuffer.wrap(bytes).asDoubleBuffer();
    double[] doubles = new double[doubleBuf.remaining()];
    doubleBuf.get(doubles);

    if (doubles.length == 0) {
      return Collections.emptyList();
    }
    if (doubles.length == PlainIndexBuilder.DEFAULT_VEC_SIZE) {
      return Collections.singletonList(new ArrayVec(doubles));
    }

    if (doubles.length % PlainIndexBuilder.DEFAULT_VEC_SIZE != 0) {
      throw new IllegalArgumentException(
          String.format("Invalid buffer size ([%d])", doubles.length));
    }

    List<Vec> result = new ArrayList<>();
    for (int i = 0; i < doubles.length; i += PlainIndexBuilder.DEFAULT_VEC_SIZE) {
      result.add(
          new ArrayVec(Arrays.copyOfRange(doubles, i, i + PlainIndexBuilder.DEFAULT_VEC_SIZE)));
    }

    return result;
  }
}
