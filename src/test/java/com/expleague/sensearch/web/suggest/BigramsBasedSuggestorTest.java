package com.expleague.sensearch.web.suggest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.expleague.sensearch.Config;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BigramsBasedSuggestorTest extends Assert{
	private static void incInMap(Map<String, Integer> m, String key) {
	    Integer currVal = m.get(key);

	    if (currVal == null) {
	      m.put(key, 0);
	    } else {
	      m.put(key, currVal + 1);
	    }
	  }
	
	static void flushBigrams(String title, Map<String, Integer> map) {
		String t = title.toLowerCase();
		String[] words = t.split("[^a-zA-Zа-яА-ЯЁё]+");
		for (int i = 0; i < words.length - 1; i++) {
			if (words[i].isEmpty()) {
				continue;
			}
			String bigram = words[i] + " " + words[i + 1];
			incInMap(map, bigram);
		}
	}
	
	ObjectMapper mapper = new ObjectMapper();
	final String suggMapfilename = "suggTmpMap";

	@Before
	public void buildMap() throws JsonGenerationException, JsonMappingException, IOException {
		Map<String, Integer> map = new TreeMap<>();
		
		flushBigrams("abc def egh", map);
		flushBigrams("ёлка палка", map);
		flushBigrams("ёлка палка", map);
		
		mapper.writeValue(new File(suggMapfilename), map);
	}
	
	@After
	public void cleanUp() throws IOException {
		Files.delete(Paths.get(suggMapfilename));
	}
	
	@Test
	public void testSuggester() throws IOException {
		class TestConf extends Config {
			@Override
			public Path getBigramsFileName() {
				return Paths.get(suggMapfilename);
			}
		}
		
		BigramsBasedSuggestor suggestor = new BigramsBasedSuggestor(new TestConf());
		
		assertEquals(Arrays.asList(),
				suggestor.getSuggestions(""));
		
		assertEquals(Arrays.asList("abc def"),
				suggestor.getSuggestions("a"));
		
		assertEquals(Arrays.asList("abc def"),
				suggestor.getSuggestions("abc"));
		
		assertEquals(Arrays.asList("abc def"),
				suggestor.getSuggestions("abc "));
		
		assertEquals(Arrays.asList("abc def"),
				suggestor.getSuggestions("abc d"));
		
		assertEquals(Arrays.asList("ёлка палка"),
				suggestor.getSuggestions("ёлка па"));
	}
}
