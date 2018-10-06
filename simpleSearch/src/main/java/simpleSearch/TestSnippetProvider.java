package simpleSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestSnippetProvider {
	List<String> getSuitableSnippets(String ignored) {
		if (ignored != null && !ignored.equals("")) {
			return Arrays.asList("Предложение 1", "Предложение 2", "Предложение 3\n с продолжением.");
		}
		return new ArrayList<>();
	}
}
