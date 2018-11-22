package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

public final class ByteTools {

  private ByteTools() {
  }

  public static byte[] toBytes(Vec vec) {
    return null;
  }

  public static Vec toVec(byte[] bytes) {
    return null;
  }

  public static byte[] toBytes(long[] lArr) {
    // TODO: need check for array size to be within int bounds
    byte[][] byteArrays = new byte[lArr.length][];
    for (int i = 0; i < lArr.length; ++i) {
      byteArrays[i] = Longs.toByteArray(lArr[i]);
    }
    return Bytes.concat(byteArrays);
  }

  public static long[] toLongArray(byte[] bytes) {
    //TODO: implement
    return new long[]{0};
  }
}
