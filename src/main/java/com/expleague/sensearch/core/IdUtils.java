package com.expleague.sensearch.core;

public class IdUtils {
  private static final int BITS_FOR_TYPE_OF_FEATURE = 5;
  private static final int BITS_FOR_NUMBER_OF_FEATURE = 15;
  public static final int BITS_FOR_FEATURES = BITS_FOR_TYPE_OF_FEATURE + BITS_FOR_NUMBER_OF_FEATURE;

  private static final long SEC_TITLE_ID = 1L;
  private static final long SEC_TEXT_ID = 2L;
  private static final long LINK_ID = 3L;

  private static final long START_SEC_TITLE_PREFIX = SEC_TITLE_ID << BITS_FOR_NUMBER_OF_FEATURE;
  private static final long START_SEC_TEXT_PREFIX = SEC_TEXT_ID << BITS_FOR_NUMBER_OF_FEATURE;
  private static final long START_LINK_PREFIX = LINK_ID << BITS_FOR_NUMBER_OF_FEATURE;

  private static final long toPageMask =
      ((1L << (Long.SIZE - BITS_FOR_FEATURES)) - 1) << BITS_FOR_FEATURES;

  private IdUtils() {
  }

  public static long toStartSecTitleId(long pageId) {
    return pageId + IdUtils.START_SEC_TITLE_PREFIX;
  }

  public static long toStartSecTextId(long pageId) {
    return pageId + IdUtils.START_SEC_TEXT_PREFIX;
  }

  public static long toStartLinkId(long pageId) {
    return pageId + IdUtils.START_LINK_PREFIX;
  }

  public static long toPageId(long id) {
    return id & toPageMask;
  }

  private static long getFeatureTypeId(long id) {
    return (id >> BITS_FOR_NUMBER_OF_FEATURE) & ((1 << BITS_FOR_TYPE_OF_FEATURE) - 1);
  }

  public static boolean isSecTitleId(long id) {
    return getFeatureTypeId(id) == SEC_TITLE_ID;
  }

  public static boolean isSecTextId(long id) {
    return getFeatureTypeId(id) == SEC_TEXT_ID;
  }

  public static boolean isLinkId(long id) {
    return getFeatureTypeId(id) == LINK_ID;
  }

  public static boolean isPageTitleId(long id) {
    return id == toPageId(id);
  }
}
