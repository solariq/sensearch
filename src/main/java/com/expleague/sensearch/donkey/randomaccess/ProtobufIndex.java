package com.expleague.sensearch.donkey.randomaccess;

import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

public abstract class ProtobufIndex<K, V extends GeneratedMessage> extends LevelDbBasedIndex<K, V> {
  private static final String DECODER_METHOD_NAME = "parseFrom";
  private static final String ENCODER_METHOD_NAME = "toByteArray";

  private final Class<V> valueClass;
  private final Method bytesDecoder;
  private final Method bytesEncoder;
  public ProtobufIndex(Path root, Class<V> valueClass) {
    super(root);
    this.valueClass = valueClass;
    try {
      bytesDecoder = valueClass.getMethod(DECODER_METHOD_NAME, byte[].class);
      bytesEncoder = valueClass.getMethod(ENCODER_METHOD_NAME);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(String.format(
          "Class [ %s ] is not a valid protobuf class. Decoder method [ %s(byte[]) ] and"
              + "encoder method [ %s() ] were not found",
          valueClass.getSimpleName(), DECODER_METHOD_NAME, ENCODER_METHOD_NAME), e);
    }
  }

  @Override
  protected V decodeValue(byte[] bytes) {
    try {
      return valueClass.cast(bytesDecoder.invoke(null, bytes));
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected byte[] encodeValue(V value) {
    try {
      return (byte[]) bytesEncoder.invoke(value);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
