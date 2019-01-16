package com.expleague.sensearch.donkey.plain;

import com.google.common.primitives.Longs;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IdGenerator {

  public long termId(String term) {
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

  private long getMd5HighBytes(String string) {
    MessageDigest md5;

    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    md5.update(string.getBytes());
    byte[] digest = md5.digest();
    return Longs.fromByteArray(digest);
  }

  private long idFromUri(URI uri) {
    long md5HighBytes = getMd5HighBytes(uri.toString());
    return md5HighBytes == 0 ? -1 : -Math.abs(md5HighBytes);
  }
}
