package components.crawler;

import components.crawler.document.CrawlerDocument;
import components.crawler.document.XMLParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CrawlerXML implements Crawler {

    private Path path;
    private XMLParser parser = new XMLParser();

    public CrawlerXML(Path path) {
        this.path = path;
    }

    @Override
    public Stream<CrawlerDocument> makeStream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new DocumentIterator(path),
                        Spliterator.ORDERED | Spliterator.SORTED),
                false);
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

                    //Need refactor
                    for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                        fileOutputStream.write(c);
                    }

                    fileOutputStream.flush();
                    zipInputStream.closeEntry();
                    fileOutputStream.close();
                    File file = new File(fileName);

                    CrawlerDocument result = parser.parseXML(file);

                    file.deleteOnExit();

                    return result;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

class DocumentIterator implements Iterator<CrawlerDocument> {

    private Path path;
    private ZipInputStream zipInputStream;
    private ZipEntry zipEntry;
    private ZipEntry nextZipEntry;
    private XMLParser parser = new XMLParser();

    DocumentIterator(Path path) {
        try {
            this.path = path;
            init();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void init() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(path.toString());
        zipInputStream = new ZipInputStream(fileInputStream);
        try {
            zipEntry = zipInputStream.getNextEntry();
            nextZipEntry = zipInputStream.getNextEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return nextZipEntry != null;
    }

    @Override
    public CrawlerDocument next() {
        try {
             while (zipEntry.isDirectory()) {
                zipEntry = nextZipEntry;
                nextZipEntry = zipInputStream.getNextEntry();
            }
            String fileName = "../WikiDocs/tmp/";
            Files.createDirectories(Paths.get(fileName));
            String[] n = zipEntry.getName().split("/");
            fileName += n[1];

            FileOutputStream fileOutputStream = new FileOutputStream(fileName);

            //Need refactor
            for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                fileOutputStream.write(c);
            }

            fileOutputStream.flush();
            zipInputStream.closeEntry();
            fileOutputStream.close();
            File file = new File(fileName);

            CrawlerDocument result = parser.parseXML(file);

            file.delete();

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            zipEntry = nextZipEntry;
            try {
                nextZipEntry = zipInputStream.getNextEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
