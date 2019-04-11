package com.expleague.sensearch.experiments.joom;

import java.net.URI;

public class JoomUtils {

  public static URI uriForId(String id) {
    return URI.create("https://www.joom.com/en/products/" + id);
  }
}
