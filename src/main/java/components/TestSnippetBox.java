package components;

import components.snippeter.snippet.Snippet;

public class TestSnippetBox implements SnippetBox{
	private TestSnippetProvider provider = new TestSnippetProvider();
	CharSequence s;
	
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 3;
	}

	@Override
	public boolean makeQuery(CharSequence s) {
		// TODO Auto-generated method stub
		this.s = s;
		return true;
	}

	@Override
	public Snippet getSnippet(int idx) {
		// TODO Auto-generated method stub
		return provider.getSuitableSnippets(s);
	}

}
