package com.expleague.sensearch.core.lemmer;

import com.expleague.commons.text.lemmer.WordInfo;

public interface Lemmer {

  WordInfo parse(CharSequence seq);

}
