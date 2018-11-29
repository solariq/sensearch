package com.expleague.sensearch.donkey.crawler.document;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.donkey.crawler.document.WikiPage.WikiLink;
import com.expleague.sensearch.donkey.crawler.document.WikiPage.WikiSection;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

public class XMLParser {

  public WikiPage parseXML(File file) {
    WikiPage page = new WikiPage();
    try {
      JAXBContext context = JAXBContext.newInstance(XmlPageRootElement.class);
      Unmarshaller um = context.createUnmarshaller();
      XmlPageRootElement element = (XmlPageRootElement) um.unmarshal(file);
      XmlPage xmlPage = element.page;

      page.setTitle(xmlPage.title);
      if (xmlPage.categories == null) {
        page.setCategories(new ArrayList<>());
      } else {
        page.setCategories(Arrays.asList(xmlPage.categories.split("@")));
      }
      page.setId(xmlPage.id);

      List<Section> sections =
          xmlPage
              .sections
              .stream()
              .map(
                  xmlSection -> {
                    List<Link> links = new ArrayList<>();
                    StringBuilder text = new StringBuilder();

                    if (xmlSection.content != null) {
                      for (Serializable serializable : xmlSection.content) {
                        if (serializable instanceof String) {
                          text.append(((String) serializable).trim());
                        } else if (serializable instanceof XmlSectionLink) {
                          XmlSectionLink link = (XmlSectionLink) serializable;

                          if (link.targetId == 0) {
                            link.targetId = -1;
                          }
                          if (link.targetTitle == null) {
                            link.targetTitle = "";
                          }

                          if (text.length() > 0 && text.charAt(text.length() - 1) != ' ') {
                            text.append(" ");
                          }
                          links.add(
                              new WikiLink(
                                  link.text, link.targetTitle, link.targetId, text.length()));
                          text.append(link.text.trim());
                          text.append(" ");
                        }
                      }
                    }

                    return new WikiSection(
                        text, Arrays.asList(xmlSection.title.split("\\|@\\|")), links);
                  })
              .collect(Collectors.toList());

      page.setSections(sections);
    } catch (JAXBException e) {
      throw new IllegalArgumentException(e);
    }

    return page;
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

  @XmlRootElement(name = "pages")
  private static class XmlPageRootElement {
    @XmlElement(name = "page")
    XmlPage page;
  }

  @XmlRootElement(name = "page")
  private static class XmlPage {
    @XmlAttribute(name = "id")
    long id;

    @XmlAttribute(name = "title")
    String title;

    @XmlAttribute(name = "categories")
    String categories;

    @XmlElementWrapper(name = "sections")
    @XmlElement(name = "section")
    List<XmlSection> sections;
  }

  @XmlRootElement(name = "section")
  private static class XmlSection {
    @XmlAttribute(name = "title")
    String title;

    @XmlElementRef(name = "link", type = XmlSectionLink.class)
    @XmlMixed
    List<Serializable> content;
  }

  @XmlRootElement(name = "link")
  private static class XmlSectionLink implements Serializable {

    @XmlValue
    String text;

    @XmlAttribute(name = "target")
    String targetTitle;

    @XmlAttribute(name = "targetId")
    long targetId;
  }
}
