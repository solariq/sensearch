package components.index;

/**
 * Expected that class that implements interface may be created only by the class that implements
 * Index interface
 */
public interface Document {
  long getId();
  CharSequence getContent();
  CharSequence getTitle();
}
