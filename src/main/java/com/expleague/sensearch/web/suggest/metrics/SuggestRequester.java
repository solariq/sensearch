package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class SuggestRequester {
	public final String suggestUrl =
			"https://suggest.yandex.ru/suggest-ya.cgi?v=4&part=";

	public List<String> getSuggests(String partialQuery) throws IOException {
		URL url = new URL(suggestUrl + URLEncoder.encode(partialQuery));

		ObjectMapper mapper = new ObjectMapper();
		
		List<String> suggests = new ArrayList<String>();
		
		JsonNode root = mapper.readTree(url.openStream());

		Iterator<JsonNode> it = root.get(1).iterator();
		while(it.hasNext()) {
			suggests.add(it.next().asText());
		}
		
		return suggests;
	}

	public static void main(String[] args) throws IOException {
		SuggestRequester requester = new SuggestRequester();

		for (String suggest : requester.getSuggests("wiki алек")) {
			System.out.println(suggest);
		}
		
	}
}
