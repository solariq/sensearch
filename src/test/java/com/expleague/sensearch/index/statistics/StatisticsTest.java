package com.expleague.sensearch.index.statistics;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import com.expleague.sensearch.Constants;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class StatisticsTest {
	
	private CrawlerDocument d1, d2;
	
	@Before
	public void prepare() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.readValue(new File("./paths.json"), Constants.class);
		
		d1 = new CrawlerDocument() {
			
			@Override
			public String getTitle() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Long getID() {
				return 1l;
			}
			
			@Override
			public CharSequence getContent() {
				return "a b c d";
			}
		};
		d2 = new CrawlerDocument() {
			
			@Override
			public String getTitle() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Long getID() {
				return 2l;
			}
			
			@Override
			public CharSequence getContent() {
				return "ab b k";
			}
		};
		
		Stats stats = new Stats();
		stats.acceptDocument(d1);
		stats.acceptDocument(d2);
		
		stats.writeToFile(Constants.getStatisticsFileName());
	}
	
	@Test
	public void testCreateSerializeRead() throws JsonGenerationException, JsonMappingException, IOException {
		Stats stats = Stats.readStatsFromFile(Constants.getStatisticsFileName());
		
		assertEquals(Long.valueOf(1), stats.getNumberOfDocumentsWithWord().get("a"));
		assertEquals(Long.valueOf(2), stats.getNumberOfWordOccurences().get("b"));
		assertEquals(Long.valueOf(1), stats.getTermFrequencyInDocument("b", 2l));
		
		assertEquals(Long.valueOf(4), stats.getDocumentLength().get(1l));
		assertEquals(Long.valueOf(3), stats.getDocumentLength().get(2l));
		
		assertEquals(2l, stats.getTotalNumberOfDocuments());
		assertEquals(7l, stats.getTotalLength());
	}
}
