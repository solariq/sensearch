package com.expleague.sensearch.snippet;

import com.expleague.sensearch.index.IndexedDocument;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.SenSeArch;

import java.util.ArrayList;
import java.util.List;

public class SnippetBoxImpl implements SnippetBox {

  final private SnippetsCreator snippetsCreator = new SnippetsCreator();
  private SenSeArch searcher;
  private Query query;
  private List<IndexedDocument> docList = new ArrayList<>();
  private ArrayList<Snippet> snippets = new ArrayList<>();

  public SnippetBoxImpl(SenSeArch searcher) {
    this.searcher = searcher;
  }

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
