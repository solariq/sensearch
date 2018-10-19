package components.crawler.document;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/*
<pages>
<page id="7" title="Литва" revision="1483089236000" type="text/x-wiki" ns-id="0" ns-name="">
 */

public class XMLParser {

    public WikiPage parseXML(File file) {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        WikiPage page = new WikiPage();
        try {
            XMLEventReader reader = xmlInputFactory.
                    createXMLEventReader(new FileInputStream(file.getAbsolutePath()));
            StringBuilder text = null;
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    StartElement startElement = xmlEvent.asStartElement();
                    if (startElement.getName().getLocalPart().equals("page")) {
                        page = new WikiPage();
                        text = new StringBuilder();

                        Attribute idAttr = startElement.getAttributeByName(new QName("id"));
                        if (idAttr != null) {
                            page.setId(Integer.parseInt(idAttr.getValue()));
                        }

                        Attribute titleAttr = startElement.getAttributeByName(new QName("title"));
                        if (titleAttr != null) {
                            page.setTitle(titleAttr.getValue());
                        }

                        /*Attribute a;
                        a = startElement.getAttributeByName(new QName("revision"));
                        if (a != null) {
                            page.revision = a.getValue();
                        }

                        a = startElement.getAttributeByName(new QName("type"));
                        if (a != null) {
                            page.type = a.getValue();
                        }

                        a = startElement.getAttributeByName(new QName("ns-id"));
                        if (a != null) {
                            page.nsId = a.getValue();
                        }*/

                        while (reader.hasNext() && (xmlEvent = reader.nextEvent()).isCharacters()) {
                            text.append(xmlEvent.asCharacters().getData());
                        }
                    }
                }
                if (xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    if (endElement.getName().getLocalPart().equals("page")) {
                        page.setPage(text);
                    }
                }
            }
            //writeXML(page);
            return page;
        } catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
    private void writeXML(WikiPage page) {
        String fileName = "/home/artem/JetBrains/WikiDocs/Mini_Wiki/" + page.getID() + ".xml";
        String startElement = "page";

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.
                    createXMLStreamWriter(new FileOutputStream(fileName), "UTF-8");
            xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
            xmlStreamWriter.writeCharacters("\n");

            xmlStreamWriter.writeStartElement("Pages");
            xmlStreamWriter.writeCharacters("\n");

            xmlStreamWriter.writeStartElement(startElement);
            xmlStreamWriter.writeAttribute("id", Long.toString(page.getID()));
            xmlStreamWriter.writeAttribute("title", page.getTitle());
            xmlStreamWriter.writeAttribute("revision", page.revision);
            xmlStreamWriter.writeAttribute("type", page.type);
            xmlStreamWriter.writeAttribute("ns-id", page.nsId);
            xmlStreamWriter.writeAttribute("ns-name", "");
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeCharacters(page.getContent().toString());
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeEndElement();

            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndDocument();

            xmlStreamWriter.flush();
            xmlStreamWriter.close();


        } catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
    }//*/
}