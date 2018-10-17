package components.snippeter.snippet;

import components.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Maxim on 10.10.2018.
 * Email: alvinmax@mail.ru
 */
public class Passage {
    private CharSequence sentence;
    private List<Segment> selection = new ArrayList<>();
    private long rating;

    public Passage(CharSequence sentence, Query query) {
        this.sentence = sentence;
        rating = query.getTerms().stream().filter(x -> contains(sentence, x.getRaw())).count();
        Collections.sort(selection, (x, y) -> x.getLeft() < y.getLeft() ? -1 : 0);
    }

    public long getRating() {
        return rating;
    }

    public CharSequence getSentence() {
        return sentence;
    }

    public List<Segment> getSelection() {
        return selection;
    }

    private boolean contains(CharSequence s, CharSequence t) {
        int n = s.length();
        int m = t.length();

        boolean ok = false;
        for (int i = 0; i < n - m + 1; ++i) {
            if (s.subSequence(i , i + m).equals(t)) {
                ok = true;
                selection.add(new Segment(i, i + m));
            }
        }
        return ok;
    }
}
