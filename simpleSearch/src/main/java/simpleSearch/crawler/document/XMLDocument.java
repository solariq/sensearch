package simpleSearch.crawler.document;

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

/*
<pages>
<page id="7" title="Литва" revision="1483089236000" type="text/x-wiki" ns-id="0" ns-name="">
 */

public class XMLDocument implements CrawlerDocument {

    File file;
    Path path;

    XMLDocument(Path path) {
        file = new File(path.toString());
        this.path = path;
        parseXML();
    }

    private void parseXML() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        WikiPage page = null;
        try {
            XMLEventReader reader = xmlInputFactory.
                    createXMLEventReader(new FileInputStream(path.toString()));
            int i = 0;
            while (reader.hasNext() && i <= 3) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    i++;
                    StartElement startElement = xmlEvent.asStartElement();
                    if (startElement.getName().getLocalPart().equals("page")) {
                        page = new WikiPage();

                        Attribute idAttr = startElement.getAttributeByName(new QName("id"));
                        if (idAttr != null) {
                            page.setId(Integer.parseInt(idAttr.getValue()));
                        }

                        Attribute titleAttr = startElement.getAttributeByName(new QName("title"));
                        if (titleAttr != null) {
                            page.setTitle(titleAttr.getValue());
                        }
                        
                        xmlEvent = reader.nextEvent();

                        page.setPage(xmlEvent.asCharacters().getData());

                        System.err.println(page.toString());
                    } else if (xmlEvent.isEndElement()) {
                        EndElement endElement = xmlEvent.asEndElement();
                        if (endElement.getName().getLocalPart().equals("page")) {
                            //end of parse
                        }
                    }
                }
            }
        } catch (FileNotFoundException | XMLStreamException e) {
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
    public List<String> returnSentences(String word) {
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
