package components.snippeter.snippet;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class Snippet {
    private CharSequence title;
    private Cluster content;

    public Snippet(CharSequence title, Cluster content) {
        this.title = title;
        this.content = content;
    }

    public CharSequence getTitle() {
        return title;
    }

    public Cluster getContent() {
        return content;
    }
}
