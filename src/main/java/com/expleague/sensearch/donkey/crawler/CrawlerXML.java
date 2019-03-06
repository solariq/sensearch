package com.expleague.sensearch.donkey.crawler;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.DTD;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;

import com.expleague.sensearch.core.Annotations.DataZipPath;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.XMLParser;
import com.expleague.sensearch.donkey.crawler.document.XMLParser.XmlPage;
import com.expleague.sensearch.donkey.crawler.document.XMLParser.XmlPageRootElement;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.log4j.Logger;

public class CrawlerXML implements Crawler {

  private static final Logger LOG = Logger.getLogger(Crawler.class.getName());
  private final Path path;

  @Inject
  public CrawlerXML(@DataZipPath Path zipPath) {
    this.path = zipPath;
  }

  @Override
  public Stream<CrawlerDocument> makeStream() throws IOException {
    return makeRawStream().map(new XMLParser()::parseXML);
  }

  private Stream<XmlPage> makeRawStream() throws IOException {
    if (path.toString().endsWith(".zip")) {
      return StreamSupport.stream(
          Spliterators.spliteratorUnknownSize(
              new MultipleDocumentIterator(path), Spliterator.ORDERED | Spliterator.SORTED),
          false);
    } else {
      return StreamSupport.stream(
          Spliterators.spliteratorUnknownSize(
              new OneDocumentIterator(path), Spliterator.ORDERED | Spliterator.SORTED),
          false);
    }
  }

  // TODO(tehnar): there is a mess in this code, refactor it later
  class OneDocumentIterator implements Iterator<XmlPage> {

    private final XMLParser parser = new XMLParser();
    private final XMLStreamReader reader;
    private GzipCompressorInputStream zipInputStream;

    OneDocumentIterator(Path path) throws IOException {
      this.zipInputStream = new GzipCompressorInputStream(new FileInputStream(path.toString()));
      try {
        this.reader = XMLInputFactory.newInstance().createXMLStreamReader(zipInputStream);
        skipElements(START_DOCUMENT, DTD);
        reader.nextTag();
      } catch (XMLStreamException e) {
        throw new RuntimeException(e);
      }
    }

    void skipElements(Integer... elements) throws XMLStreamException {
      int eventType = reader.getEventType();

      List<Integer> types = Arrays.asList(elements);
      while (types.contains(eventType)) {
        eventType = reader.next();
      }
    }

    @Override
    public boolean hasNext() {
      try {
        return reader.hasNext();
      } catch (XMLStreamException e) {
        e.printStackTrace();
        return false;
      }
    }

    @Override
    public XmlPage next() {
      XmlPage doc = parser.parseXMLToRaw(reader);
      try {
        skipElements(CHARACTERS, END_ELEMENT);
      } catch (XMLStreamException e) {
        throw new RuntimeException(e);
      }
      return doc;
    }
  }

  class MultipleDocumentIterator implements Iterator<XmlPage> {
    private final XMLParser parser = new XMLParser();
    private ZipInputStream zipInputStream;
    private ZipEntry zipEntry;

    MultipleDocumentIterator(Path path) throws IOException {
      zipInputStream = new ZipInputStream(new FileInputStream(path.toString()));
    }

    @Override
    public boolean hasNext() {
      if (this.zipEntry == null) {
        try {
          zipEntry = zipInputStream.getNextEntry();
          while (zipEntry != null && zipEntry.isDirectory()) {
            zipEntry = zipInputStream.getNextEntry();
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      return this.zipEntry != null;
    }

    ByteArrayOutputStream temp = new ByteArrayOutputStream();
    final byte[] buffer = new byte[4096];

    @Override
    public XmlPage next() {
      try {
        if (!hasNext()) {
          return null;
        }
        temp.reset();
        while (zipInputStream.available() > 0) {
          final int read = zipInputStream.read(buffer);
          if (read < 0) {
            break;
          }
          temp.write(buffer, 0, read);
        }
        XmlPage result = null;
        try {
          Object res = parser.parseXMLToRaw(new ByteArrayInputStream(temp.toByteArray()));
          if (res instanceof XmlPage) {
            result = (XmlPage) res;
          } else {
            result = ((XmlPageRootElement) res).page;
          }
        } catch (IllegalArgumentException ignored) {
          LOG.info(zipEntry.getName() + " wrong format");
        }
        zipInputStream.closeEntry();
        zipEntry = null;
        return result;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }

  List<String> TITLES =
      Arrays.asList(
          ("The Walking Dead (season 8)\n"
              + "Persephone\n"
              + "Judaism\n"
              + "Football in Spain\n"
              + "Robot-assisted surgery\n"
              + "I Love It (Icona Pop song)\n"
              + "Skywalker family\n"
              + "Zimbabwe\n"
              + "Hectare\n"
              + "Donovan Mitchell\n"
              + "The Beatles' rooftop concert\n"
              + "Luis Guillermo Solís\n"
              + "Power Rangers\n"
              + "Beverly Hills Cop (film series)\n"
              + "List of Wimbledon ladies' singles champions\n"
              + "Jinx (children's game)\n"
              + "Hickey (surname)\n"
              + "WWE Greatest Royal Rumble\n"
              + "Bugsy Malone\n"
              + "List of Toy Story characters\n"
              + "It Wasn't Me\n"
              + "St. Francis Preparatory School\n"
              + "History of the People's Republic of China\n"
              + "Take That Look Off Your Face\n"
              + "Percy Fawcett\n"
              + "Joanna Going\n"
              + "Old Course at St Andrews\n"
              + "College World Series\n"
              + "The Mother (How I Met Your Mother)\n"
              + "Grey's Anatomy (season 14)\n"
              + "Blood gas tension\n"
              + "List of Full House and Fuller House characters\n"
              + "Judge Mathis\n"
              + "The Elf on the Shelf\n"
              + "Samuel Prescott\n"
              + "Alone (TV series)\n"
              + "United States Forces Japan\n"
              + "Supreme Court of the United States\n"
              + "Tom Ellis (actor)\n"
              + "Coat of arms of South Africa\n"
              + "Election Commission of India's Model Code of Conduct\n"
              + "Now That's What I Call Music (original UK album)\n"
              + "106th Grey Cup\n"
              + "Bastille (band)\n"
              + "Arkansas River\n"
              + "The Princess Diaries 2: Royal Engagement\n"
              + "P wave (electrocardiography)\n"
              + "Star Trek: The Next Generation\n"
              + "Robin Ward (singer)\n"
              + "On Golden Pond (1981 film)\n"
              + "America's Got Talent (season 4)\n"
              + "Queen of the South (TV series)\n"
              + "List of Prime Ministers of Pakistan\n"
              + "Stephen F. Austin\n"
              + "Tobiko\n"
              + "State of Origin series\n"
              + "Life Is Strange: Before the Storm\n"
              + "Sodom and Gomorrah\n"
              + "Xbox 360\n"
              + "Philosophy of mind\n"
              + "Cristiano Ronaldo\n"
              + "Della Reese\n"
              + "New Orleans Pelicans\n"
              + "Evolution of fish\n"
              + "Faith No More\n"
              + "Gun Control Act of 1968\n"
              + "Frankenstein\n"
              + "Mirroring (psychology)\n"
              + "The Killing (U.S. TV series)\n"
              + "Church of England")
              .split("\n"));

  public void makeZIP(int num) {
    XMLParser parser = new XMLParser();
    try {
      makeRawStream()
          .forEach(
              page -> {
                String fileName = "Mini_Wiki_en/" + page.id;
                if (!TITLES.contains(page.title.trim())) {
                  return;
                }
                XmlPageRootElement xmlPageRootElement = new XmlPageRootElement();
                xmlPageRootElement.page = page;
                parser.saveXml(xmlPageRootElement, Paths.get(fileName));
              });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
