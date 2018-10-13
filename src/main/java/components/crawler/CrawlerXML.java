package components.crawler;

import components.crawler.document.CrawlerDocument;
import components.crawler.document.XMLDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CrawlerXML implements Crawler {

    private Path path;

    public CrawlerXML(Path path) {
        this.path = path;
    }

    @Override
    public long getID(CrawlerDocument crawlerDocument) {
        return crawlerDocument.getID();
    }

    @Override
    public CrawlerDocument getDocument(long iD) {
        return runLib(iD);
    }

    private CrawlerDocument runLib(long id) {
        try {
            FileInputStream fileInputStream =
                    new FileInputStream(path.toString());
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals("Split_Wiki/" + Long.toString(id) + ".xml")) {

                    String fileName = "../WikiDocs/tmp/";
                    Files.createDirectories(Paths.get(fileName));
                    fileName += Long.toString(id) + ".xml";
                    FileOutputStream fileOutputStream = new FileOutputStream(fileName);

                    for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                        fileOutputStream.write(c);
                    }

                    fileOutputStream.flush();
                    zipInputStream.closeEntry();
                    fileOutputStream.close();
                    File file = new File(fileName);

                    return new XMLDocument(file).parseXML();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
