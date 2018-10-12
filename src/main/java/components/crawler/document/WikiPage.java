package components.crawler.document;

import java.util.ArrayList;
import java.util.List;

public class WikiPage implements CrawlerDocument {

    private long id;
    private String title;
    private CharSequence page;
    /*String revision;
    String type;
    String nsId;*/

    /*long getId() {
        return id;
    }

    CharSequence getPage() {
        return page;
    }*/

    void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    void setPage(CharSequence page) {
        this.page = page;
    }

    @Override
    public boolean checkWord(String word) {
        String text = page.toString();
        return text.contains(word);
    }

    @Override
    public List<Boolean> checkWords(List<String> words) {
        String text = page.toString();
        List<Boolean> ans = new ArrayList<>();
        for (String word : words) {
            ans.add(text.contains(word));
        }
        return ans;
    }

    @Override
    public List<CharSequence> returnSentences(String word) {
        //TODO
        return null;
    }

    @Override
    public CharSequence returnContent() {
        return page;
    }

    @Override
    public Long getID() {
        return id;
    }

    @Override
    public String toString() {
        return page.toString();
    }
}
