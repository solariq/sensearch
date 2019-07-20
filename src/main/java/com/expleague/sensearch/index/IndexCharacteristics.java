package com.expleague.sensearch.index;

public interface IndexCharacteristics extends TermStatisticsBase {
  long version();
  int embeddingVectorLength();

  /**
   * @return the count of documents in index, Document is a list of sections that form
   * the whole entity
   */
  int documentsCount();

  /**
   * @return the overall count of titles in the index. This number includes also
   * titles of sections, so it is different from the documents count
   */
  int titlesCount();

  /**
   * @return the overall count of terms in all titles.
   * This number divided by titles count will give the average length of a title
   * in the index
   */
  long titleTermsCount();

  /**
   * @return the overall count of terms in all section content. This numbers does not
   * include terms of titles of sections. This number divided by documents count will
   * give the average size of a document without titles
   */
  long contentTermsCount();

  /**
   * @return the overall count of terms that forms anchor text of a link.
   * Since links are usually part of a document content, this number already is
   * taken into account in the content terms count. This number divided by
   * links count will give the average length in terms of a link anchor text
   */
  long linkTermsCount();

  /**
   * @return the overall count of links in all documents in the index
   */
  int linksCount();

  /**
   * @return the overall count of title terms of referred pages.
   * For example, in Wikipedia collection only root page can be referred so
   * this number is the sum title terms of root pages. This number divided by
   * the outgoing links count will give the average length of referred title
   */
  int targetTitleTermsCount();
}
