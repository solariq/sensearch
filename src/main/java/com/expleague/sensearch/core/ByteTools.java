package com.expleague.sensearch.core;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public final class ByteTools {

  private ByteTools() {
  }

  public static byte[] toBytes(Vec vec) {
    double[] doubles = vec.toArray();
    float[] floats = new float[doubles.length];
    for (int i = 0; i < floats.length; i++) {
      floats[i] = (float) doubles[i];
    }
    return toBytes(floats);
  }

  public static Vec toVec(byte[] bytes) {
    FloatBuffer floatBuffer = ByteBuffer.wrap(bytes).asFloatBuffer();
    float[] floats = new float[floatBuffer.remaining()];
    floatBuffer.get(floats);
    double[] doubles = new double[floats.length];
    for (int i = 0; i < doubles.length; i++) {
      doubles[i] = floats[i];
    }
    return new ArrayVec(doubles);
  }

  public static byte[] toBytes(long[] longs) {
    ByteBuffer byteBuf = ByteBuffer.allocate(Long.BYTES * longs.length);
    LongBuffer longBuf = byteBuf.asLongBuffer();
    longBuf.put(longs);
    return byteBuf.array();
  }

  public static byte[] toBytes(int[] ints) {
    ByteBuffer byteBuf = ByteBuffer.allocate(Integer.BYTES * ints.length);
    IntBuffer intBuf = byteBuf.asIntBuffer();
    intBuf.put(ints);

    return byteBuf.array();
  }

  public static byte[] toBytes(double[] doubles) {
    ByteBuffer byteBuf = ByteBuffer.allocate(Double.BYTES * doubles.length);
    DoubleBuffer doubleBuf = byteBuf.asDoubleBuffer();
    doubleBuf.put(doubles);
    return byteBuf.array();
  }

  public static byte[] toBytes(float[] floats) {
    ByteBuffer byteBuf = ByteBuffer.allocate(Float.BYTES * floats.length);
    FloatBuffer floatBuf = byteBuf.asFloatBuffer();
    floatBuf.put(floats);
    return byteBuf.array();
  }

  public static long[] toLongArray(byte[] bytes) {
    LongBuffer longBuf = ByteBuffer.wrap(bytes).asLongBuffer();
    long[] longs = new long[longBuf.remaining()];
    longBuf.get(longs);
    return longs;
  }

  public static int[] toIntArray(byte[] bytes) {
    IntBuffer intBuf = ByteBuffer.wrap(bytes).asIntBuffer();
    int[] ints = new int[intBuf.remaining()];
    intBuf.get(ints);
    return ints;
  }

  public static double[] toDoubleArray(byte[] bytes) {
    DoubleBuffer doubleBuf = ByteBuffer.wrap(bytes).asDoubleBuffer();
    double[] doubles = new double[doubleBuf.remaining()];
    doubleBuf.get(doubles);
    return doubles;
  }

  public static float[] toFloatArray(byte[] bytes) {
    FloatBuffer floatBuf = ByteBuffer.wrap(bytes).asFloatBuffer();
    float[] floats = new float[floatBuf.remaining()];
    floatBuf.get(floats);
    return floats;
  }
}
