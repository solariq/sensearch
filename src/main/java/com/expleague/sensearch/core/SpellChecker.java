package com.expleague.sensearch.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SpellChecker {

  private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS,
      new LinkedBlockingQueue<>());
  private final Writer toSpellChecker;
  private final Reader fromSpellChecker;

  public SpellChecker(Path spellCheckerExecutable, Path modelFile) {
    try {
      Process spellChecker = Runtime.getRuntime()
          .exec(spellCheckerExecutable.toString() + " correct " + " " + modelFile.toString());
      toSpellChecker = new OutputStreamWriter(spellChecker.getOutputStream(),
          StandardCharsets.UTF_8);
      fromSpellChecker = new InputStreamReader(spellChecker.getErrorStream(),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public SpellChecker(InputStream fromSpellChecker, OutputStream toSpellChecker) {
    this.toSpellChecker = new OutputStreamWriter(toSpellChecker, StandardCharsets.UTF_8);
    this.fromSpellChecker = new InputStreamReader(fromSpellChecker, StandardCharsets.UTF_8);
  }

  public CharSequence correct(CharSequence seq) {
    final Task task = new Task(seq);
    final FutureTask<CharSequence> ftask = new FutureTask<>(task, task.answer);
    executor.execute(ftask);
    try {
      final CharSequence result = ftask.get().toString();
      if (task.e instanceof RuntimeException) {
        throw (RuntimeException) task.e;
      } else if (task.e != null) {
        throw new RuntimeException(task.e);
      }
      return result;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private class Task implements Runnable {

    private final CharSequence request;
    private final StringBuilder answer = new StringBuilder();
    private Exception e;

    private Task(CharSequence request) {
      this.request = request;
    }

    @Override
    public void run() {
      try {
        toSpellChecker.append(request);
        toSpellChecker.append(" ").append("eol").append("\n");
        toSpellChecker.flush();
        BufferedReader reader = new BufferedReader(fromSpellChecker);
        String line;

        do {
          line = reader.readLine();
          if (!line.startsWith("[info]")) {
            int splitFrom = line.startsWith(">>") ? 3 : 0;
            int splitTo = line.length() - (line.endsWith("eol") ? 4 : 0);
            answer.append(line.subSequence(splitFrom, splitTo));
          }
        } while (!line.endsWith("eol"));
      } catch (Exception e) {
        this.e = e;
      }
    }
  }
}
