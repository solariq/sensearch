package com.expleague.sensearch.donkey.crawler;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.WikiPage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class CrawlerOneXML implements Crawler {

  private Path path;

  public CrawlerOneXML(Path path) {
    this.path = path;
  }

  @Override
  public Stream<CrawlerDocument> makeStream() throws FileNotFoundException, XMLStreamException {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(new XMLIterator(new File(path.toString())),
            Spliterator.ORDERED | Spliterator.SORTED),
        false
    );
  }

  class XMLIterator implements Iterator<CrawlerDocument> {

    private File file;
    private XMLEventReader reader;
    XMLEvent xmlEvent = null;

    XMLIterator(File file) throws FileNotFoundException, XMLStreamException {
      this.file = file;
      init();
    }

    private void nextEvent() {
      StartElement startElement;
      try {
        while (reader.hasNext()) {
          xmlEvent = reader.nextEvent();
          if (xmlEvent.isStartElement()) {
            startElement = xmlEvent.asStartElement();
            if (startElement.getName().getLocalPart().equals("page")) {
              break;
            }
          }
          xmlEvent = null;
        }
      } catch (XMLStreamException e) {
        e.printStackTrace();
      }
    }

    private void init() throws FileNotFoundException, XMLStreamException {
      XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
      reader = xmlInputFactory.createXMLEventReader(
          new FileInputStream(file.getAbsolutePath())
      );
      nextEvent();
    }

    @Override
    public boolean hasNext() {
      return xmlEvent != null;
    }

    @Override
    public CrawlerDocument next() {
      StringBuilder text = new StringBuilder();
      WikiPage page = new WikiPage();
      StartElement startElement = xmlEvent.asStartElement();

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

      try {
        while (reader.hasNext() && (xmlEvent = reader.nextEvent()).isCharacters()) {
          text.append(xmlEvent.asCharacters().getData());
        }
      } catch (XMLStreamException ignored) {
      }
      if (xmlEvent.isEndElement()) {
        EndElement endElement = xmlEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals("page")) {
          page.setPage(text);
        }
      }
      nextEvent();
      return page;
    }
  }


}
