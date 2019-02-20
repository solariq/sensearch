package com.expleague.sensearch.donkey.plain;

public class IdUtils {
    public static final int BITS_FOR_TYPE_OF_FEATURE = 5;
    public static final int BITS_FOR_NUMBER_OF_FEATURE = 15;
    public static final int BITS_FOR_FEATURES = BITS_FOR_TYPE_OF_FEATURE + BITS_FOR_NUMBER_OF_FEATURE;
    public static final long START_SEC_TITLE_PREFIX = 1L << BITS_FOR_NUMBER_OF_FEATURE;
    public static final long START_SEC_TEXT_PREFIX = 2L << BITS_FOR_NUMBER_OF_FEATURE;
    public static final long START_LINK_PREFIX = 3L << BITS_FOR_NUMBER_OF_FEATURE;

    private static final long toPageMask = ((1L << (Long.SIZE - BITS_FOR_FEATURES)) - 1) << BITS_FOR_FEATURES;

    private IdUtils() {}

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

    public static boolean isSecTitleId(long id) {
        return (id & START_SEC_TITLE_PREFIX) > 0;
    }

    public static boolean isSecTextId(long id) {
        return (id & START_SEC_TEXT_PREFIX) > 0;
    }

    public static boolean isLinkId(long id) {
        return (id & START_LINK_PREFIX) > 0;
    }

    public static boolean isPageTitleId(long id) {
        return id == toPageId(id);
    }
}
