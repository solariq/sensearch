package com.expleague.sensearch.core;

public class TokenIdUtils {

  public static final int PUNCTUATION_SIZE = 5_000;
  public static final int BITS_FOR_META = 8;
  private static final int FIRST_UPPERCASE = 0x00000008; //0000'0000'0000'0000'0000'0000'0000'1000
  private static final int ALL_UPPERCASE = 0x00000004;   //0000'0000'0000'0000'0000'0000'0000'0100
  private static final int PUNCTUATION = 0x00000002;     //0000'0000'0000'0000'0000'0000'0000'0010

  public static int toId(int id) {
    return id >>> BITS_FOR_META;
  }

  public static boolean isWord(int id) {
    return (id & PUNCTUATION) == 0;
  }

  /**
   * @param id real id
   * @return true if id in punctuation
   */
  public static boolean isPunct(int id) {
    return id < PUNCTUATION_SIZE;
  }

  public static boolean allUpperCase(int id) {
    return (id & ALL_UPPERCASE) != 0;
  }

  public static boolean firstUpperCase(int id) {
    return (id & FIRST_UPPERCASE) != 0;
  }

  public static int setFirstUpperCase(int id) {
    return id | FIRST_UPPERCASE;
  }

  public static int setAllUpperCase(int id) {
    return id | ALL_UPPERCASE;
  }

  public static int setPunctuation(int id) {
    return id | PUNCTUATION;
  }

}
