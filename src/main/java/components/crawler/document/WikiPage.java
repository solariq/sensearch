package components.crawler.document;

public class WikiPage {

    private long id;
    private String title;
    private CharSequence page;
    /*String revision;
    String type;
    String nsId;*/

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CharSequence getPage() {
        return page;
    }

    public void setPage(CharSequence page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return page.toString();
    }
}
