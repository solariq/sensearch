package com.expleague.sensearch.snippet;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.log4j.Logger;

public class SnippetPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(SnippetPhase.class.getName());

  private final SnippetsCreator snippetsCreator = new SnippetsCreator();
  private final int phaseId;

  @Inject
  public SnippetPhase(@Assisted int phaseId) {
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.results() != null && whiteboard.query() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Snippet phase started");
    long startTime = System.nanoTime();

    final List<Snippet> snippets = new ArrayList<>();
    for (Page doc : Objects.requireNonNull(whiteboard.results())) {
      snippets.add(
          snippetsCreator.getSnippet(doc, whiteboard.query()));
    }
    //noinspection ToArrayCallWithZeroLengthArrayArgument
    whiteboard.putSnippets(snippets.toArray(new Snippet[snippets.size()]));

    LOG.info(String
        .format("Snippet phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
