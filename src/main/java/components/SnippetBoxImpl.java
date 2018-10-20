package components;

import components.snippeter.SnippetsCreator;
import components.snippeter.snippet.Snippet;

public class SnippetBoxImpl implements SnippetBox {
    final private SnippetsCreator snippetsCreator = new SnippetsCreator();

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean makeQuery(CharSequence s) {
        return false;
    }

    @Override
    public Snippet getSnippet(int idx) {
        return null;
    }
}
