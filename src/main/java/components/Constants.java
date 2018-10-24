package components;

public class Constants {

    private static String temporaryDocuments;

    private static String temporaryBigrams;

    private static String bigramsFileName;

    private static String temporaryIndex;

    private static String mainPageHTML;

    private static String myStem;

    private static String bigramsRegexp;

    public static String getTemporaryDocuments() {
        return Constants.temporaryDocuments;
    }

    public void setTemporaryDocuments(String temporaryDocuments) {
        Constants.temporaryDocuments = temporaryDocuments;
    }

    public static String getTemporaryBigrams() {
        return Constants.temporaryBigrams;
    }

    public void setBigramsFileName(String bigramsFileName) {
        Constants.bigramsFileName = bigramsFileName;
    }

    public static String getBigramsFileName() {
        return Constants.bigramsFileName;
    }

    public void setTemporaryBigrams(String temporaryBigrams) {
        Constants.temporaryBigrams = temporaryBigrams;
    }

    public static String getTemporaryIndex() {
        return Constants.temporaryIndex;
    }

    public void setTemporaryIndex(String temporaryIndex) {
        Constants.temporaryIndex = temporaryIndex;
    }

    public static String getMainPageHTML() {
        return Constants.mainPageHTML;
    }

    public void setMainPageHTML(String mainPageHTML) {
        Constants.mainPageHTML = mainPageHTML;
    }

    public static String getMyStem() {
        return Constants.myStem;
    }

    public void setMyStem(String myStem) {
        Constants.myStem = myStem;
    }

    public static String getBigramsRegexp() {
        return Constants.bigramsRegexp;
    }

    public void setBigramsRegexp(String bigramsRegexp) {
        Constants.bigramsRegexp = bigramsRegexp;
    }
}