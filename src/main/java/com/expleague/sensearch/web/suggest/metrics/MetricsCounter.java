package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetricsCounter {
	public static void main(String[] args) throws IOException {
		PlainIndex index = null;
		Suggestor suggestor = null;
		
		Map<String, HashSet<String>> map = null;
		
		ObjectMapper mapper = new ObjectMapper();
		map = mapper.readValue(Paths.get("sugg_dataset/map").toFile(), new TypeReference<Map<String, HashSet<String>>>(){});
	
		double rrSum = 0;
		for (Entry<String, HashSet<String>> e : map.entrySet()) {
			List<String> mySugg = suggestor.getSuggestions(e.getKey());
			int pos = 1;
			for (String ms : mySugg) {
				if (e.getValue().contains(ms)) {
					rrSum += 1.0 / pos;
				}
				pos++;
			}
		}
		
		System.out.println(rrSum);
	}
}
