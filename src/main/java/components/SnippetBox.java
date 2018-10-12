package components;

import components.snippeter.snippet.Snippet;

public interface SnippetBox {
	public int size();
	
	public boolean makeQuery(CharSequence s);
	
	public Snippet getSnippet(int idx);
}
