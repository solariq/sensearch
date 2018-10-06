package simpleSearch.snippets;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class Utils {

    public static List<String> split(CharSequence text) {
        Pattern pattern = Pattern.compile("(?<=[.!?])");
        return Arrays.asList(pattern.split(text));
    }

    public static boolean contains(String word, String sentence) {
        return sentence.contains(word);
    }
}
