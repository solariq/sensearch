package com.expleague.sensearch.clicks;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClickLogger {
    private static String dataRoot = "resources/clicks/";

    public void log(String sessionId, String query, String uri) throws IOException {
        LocalDateTime ldt = LocalDateTime.now();

        int minute = ldt.getMinute();
        int delta = minute >= 30 ? minute - 30 : minute;
        ldt = ldt.minusMinutes((long) delta);

        String cluster = dataRoot + DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(ldt);

        File file = new File(cluster);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

        JSONObject click = new JSONObject();
        click.put("sessionId", sessionId);
        click.put("query", query);
        click.put("uri", uri);

        bw.write(click.toString() + "\n");
        bw.close();
    }
}
