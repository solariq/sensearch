package com.expleague.sensearch.donkey.crawler.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class XMLParser {

  public WikiPage parseXML(File file) {
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    WikiPage page = new WikiPage();
    try {
      XMLEventReader reader = xmlInputFactory.
          createXMLEventReader(new FileInputStream(file.getAbsolutePath()));
      StringBuilder text;
      while (reader.hasNext()) {
        XMLEvent xmlEvent = reader.nextEvent();
        if (xmlEvent.isStartElement()) {
          StartElement startElement = xmlEvent.asStartElement();
          if (startElement.getName().getLocalPart().equals("page")) {
            page = new WikiPage();
            text = new StringBuilder();

            Attribute idAttr = startElement
                .getAttributeByName(new QName("id"));
            if (idAttr != null) {
              page.setId(Integer.parseInt(idAttr.getValue()));
            }

            Attribute titleAttr = startElement
                .getAttributeByName(new QName("title"));
            if (titleAttr != null) {
              page.setTitle(titleAttr.getValue());
            }

            Attribute categoriesAttr = startElement
                .getAttributeByName(new QName("categories"));
            if (categoriesAttr != null) {
              page.setCategories(Arrays.asList(categoriesAttr
                  .getValue().split("@")));
            }

            // Parse <page> tags

            List<CrawlerDocument.Section> sections = new ArrayList<>();
            String sectionTitle = "";
            StringBuilder sectionText = new StringBuilder();

            while (reader.hasNext()) {
              xmlEvent = reader.nextEvent();
              if (xmlEvent.isStartElement()) {
                StartElement sectionElement = xmlEvent.asStartElement();
                if (sectionElement.getName().getLocalPart().equals("section")) {
                  sectionTitle = sectionElement
                      .getAttributeByName(new QName("title"))
                      .getValue();
                  sectionText = new StringBuilder();

                  while (reader.hasNext() && (xmlEvent = reader.nextEvent()).isCharacters()) {
                    String s = xmlEvent.asCharacters().getData();
                    sectionText.append(s);
                    // TODO (tehnar): do not duplicate data in section and in text
                    text.append(s);
                  }
                }
              }
              if (xmlEvent.isEndElement()) {
                EndElement endElement = xmlEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals("section")) {
                  sections.add(new WikiPage.WikiSection(sectionText.toString(), sectionTitle));
                  if (text.length() > 0) {
                    text.append("\n\n\n");
                  }
                }
                if (endElement.getName().getLocalPart().equals("page")) {
                  page.setPage(text.toString());
                  page.setSections(sections);
                  break;
                }
              }
            }
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
            xmlStreamWriter.writeAttribute("id", Long.toString(page.iD()));
            xmlStreamWriter.writeAttribute("title", page.title());
            xmlStreamWriter.writeAttribute("revision", page.revision);
            xmlStreamWriter.writeAttribute("type", page.type);
            xmlStreamWriter.writeAttribute("ns-id", page.nsId);
            xmlStreamWriter.writeAttribute("ns-name", "");
            xmlStreamWriter.writeCharacters("\n");
            xmlStreamWriter.writeCharacters(page.content().toString());
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