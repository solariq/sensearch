package components;

import components.index.IndexedDocument;
import components.query.BaseQuery;
import components.query.Query;
import components.searcher.Searcher;
import components.snippeter.SnippetsCreator;
import components.snippeter.snippet.Snippet;

import java.util.ArrayList;
import java.util.List;

public class SnippetBoxImpl implements SnippetBox {

  final private SnippetsCreator snippetsCreator = new SnippetsCreator();
  private Searcher searcher;

  public SnippetBoxImpl(Searcher searcher) {
    this.searcher = searcher;
  }

  private Query query;
  private List<IndexedDocument> docList = new ArrayList<>();
  private ArrayList<Snippet> snippets = new ArrayList<>();

  @Override
  public int size() {
    return docList.size();
  }

  @Override
  public boolean makeQuery(CharSequence s) {
    docList.clear();
    snippets.clear();
    query = new BaseQuery(s);
    docList = searcher.getRankedDocuments(query);
    for (IndexedDocument doc : docList) {
      snippets.add(snippetsCreator.getSnippet(doc, query));
    }
    return true;
  }

  @Override
  public Snippet getSnippet(int idx) {
    return snippets.get(idx);
  }
}
