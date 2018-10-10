package components.snippeter.snippet;

import components.query.Query;

/**
 * Created by Maxim on 10.10.2018.
 * Email: alvinmax@mail.ru
 */
public class Passage {
    CharSequence sentence;
    long rating;

    public Passage(CharSequence sentence, Query query) {
        this.sentence = sentence;
        rating = query.getTerms().stream().filter(x -> contains(sentence, x.getRaw())).count();
    }

    public long getRating() {
        return rating;
    }

    public CharSequence getSentence() {
        return sentence;
    }

    private boolean contains(CharSequence s, CharSequence t) {
        int n = s.length();
        int m = t.length();

        for (int i = 0; i < n - m + 1; ++i) {
            if (s.subSequence(i , i + m).equals(t)) {
                return true;
            }
        }
        return false;
    }
}
