package components.index;

import java.io.IOException;

/**
 * Expected that class that implements interface may be created only by the class that implements
 * Index interface
 */
public interface IndexedDocument {

  long getId();

  CharSequence getContent();

  CharSequence getTitle();
}
