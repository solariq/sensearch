package com.expleague.sensearch;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.*;

public class SpellChecker {
  private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, new LinkedBlockingQueue<>());
  private final Writer toSpellChecker;
  private final Reader fromSpellChecker;

  public SpellChecker(Path spellCheckerExecutable, Path modelFile) {
    try {
      Process spellChecker = Runtime.getRuntime().exec(spellCheckerExecutable.toString() + " correct " + " " + modelFile.toString());
      toSpellChecker = new OutputStreamWriter(spellChecker.getOutputStream(), StandardCharsets.UTF_8);
      fromSpellChecker = new InputStreamReader(spellChecker.getInputStream(), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
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
      final CharSequence result = ftask.get();
      if (task.e instanceof RuntimeException)
        throw (RuntimeException) task.e;
      else if (task.e != null)
        throw new RuntimeException(task.e);
      return result;
    }
    catch (InterruptedException | ExecutionException e) {
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
        toSpellChecker.append(" ").append("\n");
        toSpellChecker.flush();
        CharBuffer buffer = CharBuffer.allocate(1024);

        while(fromSpellChecker.read(buffer) >= 0) {
          answer.append(buffer);
          buffer.clear();
        }
      }
      catch (Exception e) {
        this.e = e;
      }
    }
  }
}
