package com.expleague.sensearch.index;

import com.expleague.sensearch.Page;

/**
 * Expected that class that implements interface may be created only by the class that implements
 * Index interface
 */
public interface IndexedPage extends Page {

  long id();
}
