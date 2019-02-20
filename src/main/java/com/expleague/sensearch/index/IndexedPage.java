package com.expleague.sensearch.index;

import com.expleague.sensearch.Page;
import java.util.stream.LongStream;

/**
 * Expected that class that implements interface may be created only by the class that implements
 * Index interface
 */
public interface IndexedPage extends Page {
  long id();
  long parentId();
  LongStream subpagesIds();

  double getTitleDist();
  double getBodyDist();
  double getLinkDist();

  void setTitleDist(double d);
  void setBodyDist(double d);
  void setLinkDist(double d);
}
