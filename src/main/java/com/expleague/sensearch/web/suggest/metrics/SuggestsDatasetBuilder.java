package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SuggestsDatasetBuilder {
	public static void main(String[] args) throws IOException {
		SuggestRequester sreq = new SuggestRequester();
		
		Map<String, List<String>> map = new HashMap<>();
		
		Files.lines(Paths.get("wordstat/queries.txt"))
		.map(s -> s.split(" "))
		.filter(s -> s.length >= 2 && s[1].length() > 0)
		.distinct()
		.forEach(s -> {
			for (int i = 1; i < s[1].length(); i++) {
				String partQuery = s[0] + " " + s[1].substring(0, i);
				try {
					Thread.sleep(500);
					map.put(partQuery, sreq.getSuggests(partQuery));
					System.out.println(partQuery + " processed");
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		System.out.println("Number of queries: " + map.size());
		
		ObjectMapper mapper = new ObjectMapper();
		Files.createDirectories(Paths.get("sugg_dataset"));
		mapper.writeValue(Paths.get("sugg_dataset/map").toFile(), map);
	}
}
