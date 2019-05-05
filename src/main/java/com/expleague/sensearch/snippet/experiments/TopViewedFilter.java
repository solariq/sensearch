package com.expleague.sensearch.snippet.experiments;

import com.expleague.commons.util.Pair;

import java.io.*;
import java.util.*;

public class TopViewedFilter {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("./top-viewed")));
        String line;

        List<Pair<String, Integer>> articles = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String title = st.nextToken();
            Integer count = Integer.valueOf(st.nextToken());
            articles.add(new Pair<>(title, count));
        }

        articles.sort(Comparator.comparingInt(Pair::getSecond));
        Collections.reverse(articles);

        PrintWriter pw = new PrintWriter("top-viewed-" + args[0]);
        for (int i = 0; i < Integer.valueOf(args[0]); i++) {
            pw.write(articles.get(i).getFirst() + "\n");
        }

        br.close();
        pw.close();
    }
}
