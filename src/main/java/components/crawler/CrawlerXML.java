package components.crawler;

import components.crawler.document.CrawlerDocument;
import components.crawler.document.XMLParser;

import java.io.*;
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

public class CrawlerXML implements Crawler {

    private Path path;

    public CrawlerXML(Path path) {
        this.path = path;
    }

    @Override
    public Stream<CrawlerDocument> makeStream() throws FileNotFoundException {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new DocumentIterator(path),
                        Spliterator.ORDERED | Spliterator.SORTED),
                false);
    }

    class DocumentIterator implements Iterator<CrawlerDocument> {

        private Path path;
        private ZipInputStream zipInputStream;
        private ZipEntry zipEntry;
        private XMLParser parser = new XMLParser();

        DocumentIterator(Path path) throws FileNotFoundException {
            this.path = path;
            init();
        }

        private void init() throws FileNotFoundException {
            zipInputStream = new ZipInputStream(new FileInputStream(path.toString()));
            try {
                zipEntry = zipInputStream.getNextEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean hasNext() {
            return zipEntry != null;
        }

        @Override
        public CrawlerDocument next() {
            try {
                while (zipEntry.isDirectory()) {
                    zipEntry = zipInputStream.getNextEntry();
                }
                String fileName = "../WikiDocs/tmp/";
                Files.createDirectories(Paths.get(fileName));
                String[] n = zipEntry.getName().split("/");
                fileName += n[n.length - 1];

                Files.copy(zipInputStream, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
                File file = new File(fileName);

                CrawlerDocument result = parser.parseXML(file);

                if (!file.delete()) {
                    System.err.println("File " + result.getID() + ".xml isn't deleted");
                }

                return result;
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



    /*
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

                Files.copy(zipInputStream, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);

                zipInputStream.closeEntry();
                File file = new File(fileName);
                parser.parseXML(file);
                if (cnt == num) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}

