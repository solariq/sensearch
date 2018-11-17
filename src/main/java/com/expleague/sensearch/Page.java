package com.expleague.sensearch;

import java.net.URI;

public interface Page {

  URI reference();

  CharSequence title();

  CharSequence text();
}
