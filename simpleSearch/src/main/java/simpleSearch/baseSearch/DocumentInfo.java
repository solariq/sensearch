package simpleSearch.baseSearch;

import java.util.List;

/**
 * Created by sandulmv on 03.10.18.
 */
public class DocumentInfo {
  private DocumentId documentId;
  private Term foundBy;
  private long termFrequency;
  private List<Long> termPositions;
}
