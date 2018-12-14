package com.expleague.sensearch.donkey.crawler;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.WikiPage;
import com.expleague.sensearch.donkey.crawler.document.XMLParser;
import com.google.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;

public class CrawlerXML implements Crawler {

  private final Config config;
  private Path path;

  @Inject
  public CrawlerXML(Config config) {
    this.path = config.getPathToZIP();
    this.config = config;
  }

  @Override
  public Stream<CrawlerDocument> makeStream() throws IOException {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            new DocumentIterator(path, config.getTemporaryDocuments()),
            Spliterator.ORDERED | Spliterator.SORTED),
        false);
  }

  class DocumentIterator implements Iterator<CrawlerDocument> {

    private Path path;
    private Path pathTmp;
    private ZipInputStream zipInputStream;
    private ZipEntry zipEntry;
    private XMLParser parser = new XMLParser();

    DocumentIterator(Path path, Path tmpPath) throws IOException {
      this.path = path;
      pathTmp = tmpPath;
      init();
    }

    private void init() throws IOException {
      pathTmp = path.getParent().resolve(pathTmp);
      Files.createDirectories(pathTmp);
      zipInputStream = new ZipInputStream(new FileInputStream(path.toString()));
      try {
        zipEntry = zipInputStream.getNextEntry();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public boolean hasNext() {
      if (this.zipEntry == null) {
        try {
          FileUtils.deleteDirectory(pathTmp.toFile());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return this.zipEntry != null;
    }

    @Override
    public CrawlerDocument next() {
      String name = "";
      try {
        while (zipEntry.isDirectory()) {
          zipEntry = zipInputStream.getNextEntry();
        }
        name = zipEntry.getName();
        Path filePath = pathTmp.resolve(Paths.get(zipEntry.getName()).getFileName());

        Files.copy(zipInputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        File file = new File(filePath.toString());

        CrawlerDocument result = parser.parseXML(file);

        if (!file.delete()) {
          System.err.println("File " + result.id() + ".xml isn't deleted");
        }

        return result;
      } catch (IllegalArgumentException e) {
        System.err.println("File " + name + " is broken");
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          zipEntry = zipInputStream.getNextEntry();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return null;
    }
  }

  public void makeZIP(int num) {
    try {
      FileInputStream fileInputStream = new FileInputStream(path.toString());
      ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
      ZipEntry zipEntry;
      int cnt = 0;
      XMLParser parser = new XMLParser();
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        if (zipEntry.isDirectory()) {
          continue;
        }
        cnt++;
        String fileName = "../WikiDocs/Mini_Wiki/";
        Files.createDirectories(Paths.get(fileName));
        String[] n = zipEntry.getName().split("/");
        fileName += n[n.length - 1];

        if (!Files.exists(Paths.get(fileName))) {
          zipInputStream.closeEntry();
          continue;
        }
        Files.copy(zipInputStream, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);

        zipInputStream.closeEntry();
        File file = new File(fileName);
        WikiPage page = parser.parseXML(file);
        boolean[] isOk = new boolean[1];
        page.categories()
            .forEach(
                category -> {
                  isOk[0] |= category.equals("Актёры России");
                  isOk[0] |= category.equals("Футболисты России");
                  isOk[0] |= category.equals("Писатели России по алфавиту");
                  isOk[0] |= category.equals("Поэты России");
                  isOk[0] |= category.equals("Актрисы России");
                  isOk[0] |= category.equals("Мастера спорта России международного класса");
                  isOk[0] |= category.contains("Правители");

                  //          isOk[0] |= category.contains("Росс") &&
                  // !category.contains("Википедия:") && !category.contains("ПРО:");
                  isOk[0] |=
                      category.contains("Город")
                          && !category.contains("Википедия:")
                          && !category.contains("ПРО:");
                  //          isOk[0] |= category.contains("Фильм") &&
                  // !category.contains("Википедия:") && !category.contains("ПРО:");
                  //          isOk[0] |= category.contains("фильм") &&
                  // !category.contains("Википедия:") && !category.contains("ПРО:");
                });

        //        if (!isOk[0]) {
        //          Files.delete(Paths.get(fileName));
        //        }
        if (cnt % 100000 == 0) {
          System.out.println(cnt);
        }
        //        if (cnt == num) {
        //          break;
        //        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
