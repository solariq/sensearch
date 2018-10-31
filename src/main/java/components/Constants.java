package components;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {

  private static String temporaryDocuments;

  private static String temporaryBigrams;

  private static String bigramsFileName;
  
  private static String statisticsFileName;

  private static String temporaryIndex;

  private static String mainPageHTML;

  private static String myStem;

  private static String bigramsRegexp;

  private static String pathToZIP;


  public void setStatisticsFileName(String statisticsFileName) {
	Constants.statisticsFileName = statisticsFileName;
  }

  public static String getTemporaryDocuments() {
    return temporaryDocuments;
  }

  public void setTemporaryDocuments(String temporaryDocuments) {
    Constants.temporaryDocuments = temporaryDocuments;
  }

  public static String getTemporaryBigrams() {
    return temporaryBigrams;
  }

  public void setTemporaryBigrams(String temporaryBigrams) {
    Constants.temporaryBigrams = temporaryBigrams;
  }

  public static Path getBigramsFileName() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(getTemporaryBigrams())
        .resolve(bigramsFileName);
  }

  public void setBigramsFileName(String bigramsFileName) {
    Constants.bigramsFileName = bigramsFileName;
  }

  public static Path getTemporaryIndex() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(temporaryIndex);
  }

  public void setTemporaryIndex(String temporaryIndex) {
    Constants.temporaryIndex = temporaryIndex;
  }

  public static String getMainPageHTML() {
    return mainPageHTML;
  }

  public void setMainPageHTML(String mainPageHTML) {
    Constants.mainPageHTML = mainPageHTML;
  }

  public static String getMyStem() {
    return myStem;
  }

  public void setMyStem(String myStem) {
    Constants.myStem = myStem;
  }

  public static String getBigramsRegexp() {
    return bigramsRegexp;
  }

  public void setBigramsRegexp(String bigramsRegexp) {
    Constants.bigramsRegexp = bigramsRegexp;
  }

  public static Path getPathToZIP() {
    return Paths.get(pathToZIP);
  }

  public void setPathToZIP(String pathToZIP) {
    Constants.pathToZIP = pathToZIP;
  }

  public static String getStatisticsFileName() {
	return statisticsFileName;
  }

}