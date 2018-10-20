package components;

import components.index.Index;
import components.index.IndexedDocument;
import components.query.BaseQuery;
import components.query.Query;
import components.query.term.Term;
import components.searcher.FuzzySearcher;
import components.searcher.Searcher;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Searcher searcher = new FuzzySearcher(new Index() {
            @Override
            public Stream<IndexedDocument> fetchDocuments(Query query) {
                return Stream.<IndexedDocument>builder().add(new IndexedDocument() {
                    @Override
                    public long getId() {
                        return 0;
                    }

                    @Override
                    public CharSequence getContent() {
                        return "слово еще слово и еще одно слово в документе";
                    }

                    @Override
                    public CharSequence getTitle() {
                        return "странный тупой заголовок";
                    }
                }).build();
            }
        }, 4);

        searcher.getRankedDocuments(new BaseQuery("слово"));

    }
}
