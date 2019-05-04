package com.expleague.sensearch.snippet.experiments;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class TopViewed {

    private static Map<String, Integer> mp = new HashMap<>();

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("./top-viewed")));
        String line;
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String title = st.nextToken();
            int count = Integer.valueOf(st.nextToken());
            mp.put(title, count);
        }

        br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String region = st.nextToken();
            String title = st.nextToken();
            int count = Integer.valueOf(st.nextToken());
            int size = Integer.valueOf(st.nextToken());

            if (!region.equals("en")) continue;

            if (mp.containsKey(title)) {
                mp.put(title, mp.get(title) + count);
            } else {
                mp.put(title, count);
            }
        }

        PrintWriter pw = new PrintWriter("./top-viewed");
        mp.forEach((x, y) -> pw.write(x + " " + y));
        br.close();
        pw.close();
    }
}
