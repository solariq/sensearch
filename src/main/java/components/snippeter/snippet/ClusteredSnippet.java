package components.snippeter.snippet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class ClusteredSnippet implements Snippet{
    private CharSequence title;
    private Cluster content;
    private List<Segment> selection = new ArrayList<>();

    public ClusteredSnippet(CharSequence title, Cluster content) {
        this.title = title;
        this.content = content;

        int shift = 0;
        for (Passage passage : content.getPassages()) {
            int finalShift = shift;
            selection.addAll(passage.getSelection()
                    .stream()
                    .map(x -> new Segment(x.getLeft() + finalShift, x.getRight() + finalShift))
                    .collect(Collectors.toList()));
            shift += passage.getSentence().length();
        }
    }

    @Override
    public CharSequence getTitle() {
        return title;
    }

    @Override
    public CharSequence getContent() {
        return content.getPassages()
                .stream()
                .map(Passage::getSentence)
                .collect(Collectors.joining());
    }

    @Override
    public List<Segment> getSelection() {
        return selection;
    }
}
