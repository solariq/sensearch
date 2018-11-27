package com.expleague.sensearch.utils;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;

/**
 * Wrapper class for MyStem which logs all the interactions with it so these interactions can
 * be reproduced later by {@link LogBasedMyStem}
 */
public class RecordingMyStem implements MyStem {

  private MyStem myStem;

  public RecordingMyStem(Path mystemExecutable, Path logPath) {
    Process mystem;
    try {
      mystem = Runtime.getRuntime().exec(mystemExecutable.toString() + " -i --weight -c");
      OutputStream toMyStem = mystem.getOutputStream();
      InputStream fromMyStem = mystem.getInputStream();

      OutputStream fromMyStemLog = Files.newOutputStream(Paths.get(logPath + "_from"), StandardOpenOption.CREATE);
      OutputStream toMyStemLog = Files.newOutputStream(Paths.get(logPath + "_to"), StandardOpenOption.CREATE);

      InputStream fromMyStemWrapper = new TeeInputStream(fromMyStem, fromMyStemLog);
      OutputStream toMyStemWrapper = new TeeOutputStream(toMyStem, toMyStemLog);

      myStem = new MyStemImpl(fromMyStemWrapper, toMyStemWrapper);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public List<WordInfo> parse(CharSequence charSequence) {
    return myStem.parse(charSequence);
  }
}
