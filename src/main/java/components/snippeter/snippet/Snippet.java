package components.snippeter.snippet;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class Snippet {
    private String title;
    private String sentence;

    public Snippet(String title, String sentence) {
        this.title = title;
        this.sentence = sentence;
    }

    public String getTitle() {
        return title;
    }

    public String getSentence() {
        return sentence;
    }
}
