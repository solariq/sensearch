package components.suggestor;

import java.util.List;

public interface Suggestor {
	public List<String> getSuggestions(String searchString);
}
