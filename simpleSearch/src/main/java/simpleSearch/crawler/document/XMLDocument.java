package simpleSearch.crawler.document;

import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.*;
import java.io.*;

public class XMLDocument implements MyDocument {

    XMLDocument(Path path) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append("<?xml version=\"1.0\"?> <class> </class>");
            ByteArrayInputStream input = new ByteArrayInputStream(
                    xmlStringBuilder.toString().getBytes("UTF-8"));
            Document doc = builder.parse(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkWord(String word) {
        return false;
    }

    @Override
    public List<Boolean> checkWords(List<String> words) {
        return null;
    }

    @Override
    public List<String> returnSentenses(String word) {
        return null;
    }

    @Override
    public CharSequence returnContent() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }
}
