package com.expleague.sensearch.miner.features;

import com.expleague.ml.meta.DSItem;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;

@JsonPropertyOrder({"query", "uri"})
public class QURLItem extends DSItem.Stub {

  private final String query;
  private final URI uri;

  private transient Page pageCache;
  private transient Query queryCache;

  @JsonCreator
  private QURLItem(@JsonProperty("query") String query, @JsonProperty("uri") String uri) {
    this.query = query;
    this.uri = URI.create(uri);
  }

  public QURLItem(Page page, Query query) {
    this.query = query.text();
    this.uri = page.uri();
    pageCache = page;
    queryCache = query;
  }

  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("uri")
  public URI getUri() {
    return uri;
  }

  public Page pageCache() {
    return pageCache;
  }

  public Query queryCache() {
    return queryCache;
  }

  @Override
  public String id() {
    return query + "::" + uri.toString();
  }
}
