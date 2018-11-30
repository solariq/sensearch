package com.expleague.sensearch.utils;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that uses logs provided by {@link RecordingMyStem} to mock MyStem behaviour. Throws an
 * error if given a parseTextToWords request which can not be found in logs
 */
public class LogBasedMyStem implements MyStem {

  private Map<CharSequence, List<WordInfo>> requests = new HashMap<>();

  public LogBasedMyStem(Path logPath) {
    try (InputStream fromMyStem = Files.newInputStream(Paths.get(logPath + "_from"));
        InputStream toMyStem = Files.newInputStream(Paths.get(logPath + "_to"))) {
      MyStemImpl myStem =
          new MyStemImpl(
              fromMyStem,
              new OutputStream() {
                @Override
                public void write(int i) {}
              });

      BufferedReader toMyStemReader =
          new BufferedReader(new InputStreamReader(toMyStem, StandardCharsets.UTF_8));
      StringBuilder line = new StringBuilder();
      while (true) {
        String newLine = toMyStemReader.readLine();
        if (newLine == null) {
          break;
        }
        line.append(newLine).append("\n");
        if (newLine.endsWith("eol")) {
          String request = line.substring(0, line.length() - " eol\n".length());
          requests.put(request, myStem.parse(line.toString()));
          line = new StringBuilder();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public List<WordInfo> parse(CharSequence charSequence) {
    if (requests.containsKey(charSequence)) {
      return requests.get(charSequence);
    }

    throw new IllegalArgumentException(
        "Request to mystem '" + charSequence + "' can not be found in logs");
  }
}
