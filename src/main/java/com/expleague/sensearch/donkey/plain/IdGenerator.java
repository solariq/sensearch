package com.expleague.sensearch.donkey.plain;

import com.google.common.primitives.Longs;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IdGenerator {

  public long termId(CharSequence term) {
    long md5HighBytes = getMd5HighBytes(term);
    if (md5HighBytes == 0 || md5HighBytes == Long.MIN_VALUE) {
      return 1;
    }
    return Math.abs(md5HighBytes);
  }

  public long pageId(URI pageUri) {
    return idFromUri(pageUri);
  }

  public long sectionId(URI sectionUri) {
    return idFromUri(sectionUri);
  }

  private final ThreadLocal<MessageDigest> md5 = ThreadLocal.withInitial(() -> {
    try {
      return MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  });

  private long getMd5HighBytes(CharSequence string) {
    final MessageDigest md5 = this.md5.get();
    md5.reset();
    md5.update(StandardCharsets.UTF_8.encode(CharBuffer.wrap(string)));
    byte[] digest = md5.digest();
    return Longs.fromByteArray(digest);
  }

  private long idFromUri(URI uri) {
    long md5HighBytes = getMd5HighBytes(uri.toString());
    return md5HighBytes == 0 ? -1 : -Math.abs(md5HighBytes);
  }
}
