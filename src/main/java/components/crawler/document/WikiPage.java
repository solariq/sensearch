package components.crawler.document;

public class WikiPage implements CrawlerDocument {

    private long id;
    private String title;
    private CharSequence page;
    /*
    String revision;
    String type;
    String nsId;
    //*/

    void setId(long id) {
        this.id = id;
    }

    void setTitle(String title) {
        this.title = title;
    }

    void setPage(CharSequence page) {
        this.page = page;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public CharSequence getContent() {
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
