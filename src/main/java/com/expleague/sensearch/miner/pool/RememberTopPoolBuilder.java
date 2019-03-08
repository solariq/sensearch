package com.expleague.sensearch.miner.pool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class RememberTopPoolBuilder {
	
private final ObjectMapper mapper = new ObjectMapper();
		
	public abstract Path getRememberDir();
	
	private File getRememberTopFile(String queryString) {
		return getRememberDir().resolve("query_" + queryString).toFile();
	}
	
	public  List<URI> getSavedQueryTop(String queryString) {
		List<URI> res = new ArrayList<>();
		try {
			res = mapper.readValue(
					getRememberTopFile(queryString),
					new TypeReference<List<URI>>(){});
		} catch (IOException e) {}
		return res;
	}
	
	public void saveQueryTop(String queryString, List<URI> l) {
		try {
			Files.createDirectories(getRememberDir());
			mapper.writeValue(
							getRememberTopFile(queryString),
							l);
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
}
