package components.searcher;

import components.index.IndexedDocument;
import components.query.BaseQuery;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class FuzzySearcherTest {
    @Test
    public void testRelevantDocumentIsOnTop() {
        IndexedDocument relevantDocument = new IndexedDocument() {
            @Override
            public long getId() {
                return 0;
            }

            @Override
            public CharSequence getContent() {
                return "а а а а слово еще а а слово и еще одно слово в документе";
            }

            @Override
            public CharSequence getTitle() {
                return "странный тупой заголовок";
            }
        };

        IndexedDocument irrelevantDocument = new IndexedDocument() {
            @Override
            public long getId() {
                return 0;
            }

            @Override
            public CharSequence getContent() {
                return "нет слов в документе";
            }

            @Override
            public CharSequence getTitle() {
                return "еще один странный тупой заголовок";
            }
        };

        Searcher searcher = new FuzzySearcher(query -> Stream.<IndexedDocument>builder()
                .add(irrelevantDocument)
                .add(relevantDocument)
                .build(), 4);

        assertEquals(Arrays.asList(relevantDocument), searcher.getRankedDocuments(new BaseQuery("слово")));
    }
}