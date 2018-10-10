package components.snippeter.snippet;

import java.util.List;

/**
 * Created by Maxim on 10.10.2018.
 * Email: alvinmax@mail.ru
 */
public class Cluster {
    List<Passage> passages;
    long rating;

    public Cluster(List<Passage> passages) {
        this.passages = passages;
        rating = passages.stream().mapToLong(Passage::getRating).sum();
    }

    public long getRating() {
        return rating;
    }

    public List<Passage> getPassages() {
        return passages;
    }

}
