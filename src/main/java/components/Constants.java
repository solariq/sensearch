package components;

public class Constants {

    private static String temporaryDocuments;

    private static String temporaryBigrams;
    private static String bigramsFileName;

    private static String temporaryIndex;

    private static String mainPageHTML;

    public static String getTemporaryDocuments() {
        return temporaryDocuments;
    }

    public void setTemporaryDocuments(String temporaryDocuments) {
        Constants.temporaryDocuments = temporaryDocuments;
    }

    public static String getTemporaryBigrams() {
        return temporaryBigrams;
    }

    public void setBigramsFileName(String bigramsFileName) {
        Constants.bigramsFileName = bigramsFileName;
    }

    public static String getBigramsFileName() {
        return bigramsFileName;
    }

    public void setTemporaryBigrams(String temporaryBigrams) {
        Constants.temporaryBigrams = temporaryBigrams;
    }

    public static String getTemporaryIndex() {
        return temporaryIndex;
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
}