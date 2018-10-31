package components.snippeter.passage;

import components.snippeter.Segment;
import java.util.ArrayList;
import java.util.List;

public class Passages {

  public static boolean contains(CharSequence s, CharSequence t) {
    return containsSelection(s, t).size() > 0;
  }

  public static List<Segment> containsSelection(CharSequence text, CharSequence t) {
    final CharSequence s = text.toString().toLowerCase();
    int n = s.length();
    int m = t.length();

    List<Segment> selection = new ArrayList<>();

    // Алгоритм Бойера — Мура

    int[] suffixShift = new int[m + 1];
    for (int i = 0; i < m + 1; ++i) {
      suffixShift[i] = m;
    }
    int[] z = new int[m];

    for (int j = 1, maxJ = 0, maxZ = 0; j < m; ++j) {
      if (j <= maxZ) {
        z[j] = Math.min(maxZ - j + 1, z[j - maxJ]);
      }
      while (j + z[j] < m && t.charAt(m - 1 - z[j]) == t.charAt(m - 1 - (j + z[j]))) {
        z[j]++;
      }
      if (j + z[j] - 1 > maxZ) {
        maxJ = j;
        maxZ = j + z[j] - 1;
      }
    }
    for (int j = m - 1; j > 0; --j) {
      suffixShift[m - z[j]] = j;
    }
    for (int j = 1, r = 0; j <= m - 1; ++j) {
      if (j + z[j] == m) {
        for (; r <= j; r++) {
          if (suffixShift[r] == m) {
            suffixShift[r] = j;
          }
        }
      }
    }

    boolean ok = false;
    int j, bound = 0;
    for (int i = 0; i <= n - m; i += suffixShift[j + 1]) {
      for (j = m - 1; j >= bound && t.charAt(j) == s.charAt(i + j); j--) {
        ;
      }
      if (j < bound) {
        ok = true;
        if ((i == 0 || !Character.isAlphabetic(s.charAt(i - 1))) && (i + m == n || !Character
            .isAlphabetic(s.charAt(i + m)))) {
          selection.add(new Segment(i, i + m));
        }
        bound = m - suffixShift[0];
        j = -1;
      } else {
        bound = 0;
      }
    }
    return selection;
  }
}
