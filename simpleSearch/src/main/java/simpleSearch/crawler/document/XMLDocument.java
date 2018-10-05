package simpleSearch.crawler.document;

import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.List;


import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class XMLDocument implements MyDocument {

    File file;
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    XMLEventReader reader;

    XMLDocument(Path path) {
        file = new File(path.toString());
        try {
            reader = xmlInputFactory.createXMLEventReader(new FileInputStream(path.toString()));
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    StartElement startElement = xmlEvent.asStartElement();

                }
            }

        } catch (XMLStreamException | FileNotFoundException e) {
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
