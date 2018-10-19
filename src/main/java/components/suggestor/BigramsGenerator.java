package components.suggestor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import components.crawler.Crawler;
import components.crawler.CrawlerXML;
import components.crawler.document.CrawlerDocument;

public class BigramsGenerator {
	
	public static String mapPath = "suggestions_data/bigramMap";
	
	static void incInMap(Map<String, Integer> m, String key) {
		Integer currVal = m.get(key);
		
		if (currVal == null) {
			m.put(key, 0);
		} else {
			m.put(key, currVal + 1);
		}
	}
	
	public static void generateBigramsFromTitles(Path wikiPath, File output) throws JsonGenerationException, JsonMappingException, IOException {
		Crawler cr = new CrawlerXML(wikiPath);
		TreeMap<String, Integer> result = new TreeMap<>();
		
		cr.makeStream().map(CrawlerDocument::getTitle).forEach(new Consumer<String>() {

			@Override
			public void accept(String t) {
				// TODO Auto-generated method stub
				t = t.toLowerCase();
				String[] words = t.split("[^a-zA-Z]+");
				for (int i = 0; i < words.length - 1; i++) {
					if (words[i].isEmpty()) {
						continue;
					}
					String bigram = words[i] + " " + words[i + 1];
					incInMap(result, bigram);
				}
			}
			
		});
		
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.writeValue(output, result);
	}
	
	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		generateBigramsFromTitles(Paths.get("suggestions_data/Mini_Wiki.zip"), new File(mapPath));
	}
}
