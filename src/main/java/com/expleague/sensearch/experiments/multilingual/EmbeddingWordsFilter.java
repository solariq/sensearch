package com.expleague.sensearch.experiments.multilingual;

import java.io.*;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class EmbeddingWordsFilter {
    private StringTokenizer st;

    private String next() throws IOException {
        return st.nextToken();
    }

    private int nextInt() throws IOException {
        return Integer.parseInt(next());
    }

    private double nextDouble() throws IOException {
        return Double.parseDouble(next());
    }

    public static void main(String[] args) throws IOException {
        if (args == null || args.length != 2) {
            System.err.println("Usage : <path to base> <path to save filtered");
            return;
        }

        new EmbeddingWordsFilter().filter(args[0], args[1]);
    }

    private void filter(String from, String to) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(from)));
        PrintWriter pw = new PrintWriter(new File(to));
        PrintWriter log = new PrintWriter(new File("./bad_words"));

        int bad = 0;
        int all = 0;

        String line;
        while((line = br.readLine()) != null) {
            st = new StringTokenizer(line);
            String word = next();
            int n = nextInt();
            double[] x = new double[n];
            for (int i = 0; i < n; i++) {
                x[i] = nextDouble();
            }

            all++;
            if (!Pattern.matches("[\\p{Digit}\\p{javaAlphabetic}-']*", word)) {
                bad++;
                log.print(word + "\n");
                continue;
            }

            pw.print(word + "\t" + n);
            for (int i = 0; i < n; i++) {
                pw.print(" " + x[i]);
            }
            pw.print("\n");
        }

        System.out.println(bad + " / " + all + " is bad.");

        br.close();
        pw.close();
        log.close();
    }
}
