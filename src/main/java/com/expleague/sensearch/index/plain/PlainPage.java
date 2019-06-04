package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.util.cache.CacheStrategy.Type;
import com.expleague.commons.util.cache.impl.FixedSizeCache;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.base.Functions;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.ws.rs.NotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlainPage implements IndexedPage {

  public static final IndexedPage EMPTY_PAGE =
      new IndexedPage() {
        private final URI DEFAULT_URI = URI.create("https://www.wikipedia.org/");
        private static final String EMPTY_STRING = "";

        @Override
        public long id() {
          throw new NotSupportedException("Id is not determined for an EmptyPage");
        }

        @Override
        public long parentId() {
          throw new NotSupportedException("Parent id is not determined for an EmptyPage");
        }

        @Override
        public LongStream subpagesIds() {
          return LongStream.empty();
        }

        @Override
        public URI uri() {
          return DEFAULT_URI;
        }

        @Override
        public CharSequence content(SegmentType... types) {
          return EMPTY_STRING;
        }

        @Override
        public List<CharSequence> categories() {
          return Collections.emptyList();
        }

        @Override
        public Stream<Link> outgoingLinks(LinkType type) {
          return Stream.empty();
        }

        @Override
        public Stream<Link> incomingLinks(LinkType type) {
          return Stream.empty();
        }

        @Override
        public Page parent() {
          return this;
        }

        @Override
        public Page root() {
          return this;
        }

        @Override
        public boolean isRoot() {
          return true;
        }

        @Override
        public Stream<Page> subpages() {
          return Stream.empty();
        }

        @Override
        public Stream<CharSequence> sentences(SegmentType t) {
          return Stream.empty();
        }

        @Override
        public Vec titleVec() {
          return Vec.EMPTY;
        }

        @Override
        public Vec sentenceVec(CharSequence sentence) {
          return Vec.EMPTY;
        }
      };

  private static final Logger LOG = LoggerFactory.getLogger(PlainPage.class);

  private final long id;
  private final PlainIndex index;
  private final IndexUnits.Page protoPage;
  private final URI uri;
  private final boolean isEmpty;
  private Vec titleVec;
  private Map<CharSequence, Vec> sentenceVecs;

  private PlainPage(IndexUnits.Page protoPage, PlainIndex index) {
    this.index = index;
    this.protoPage = protoPage;
    this.uri = URI.create(protoPage.getUri());

    id = protoPage.getPageId();
    isEmpty = false;
  }

  private static final int CACHE_SIZE = 4 * (1 << 10); // 4K pages

  private static final FixedSizeCache<Long, IndexedPage> vecCache =
      new FixedSizeCache<>(CACHE_SIZE, Type.LRU);

  public static IndexedPage create(long id, PlainIndex plainIndex) {
    return vecCache.get(
        id,
        (id1) -> {
          try {
            IndexUnits.Page protoPage = plainIndex.protoPageLoad(id);
            return new PlainPage(protoPage, plainIndex);
          } catch (NoSuchElementException e) {
            LOG.warn(
                String.format(
                    "No page was found in the index by given id [ %d ]." + " Returned empty page",
                    id));
            return EMPTY_PAGE;
          } catch (InvalidProtocolBufferException e) {
            LOG.warn(
                String.format(
                    "Encountered invalid protobuf for the page with id [ %d ]."
                        + " Empty page is returned. Cause: %s",
                    id, e.toString()));
            return EMPTY_PAGE;
          }
        });
  }

  @Override
  public long id() {
    if (isEmpty) {
      throw new UnsupportedOperationException("There is no page id for an empty page!");
    }
    return this.id;
  }

  @Override
  public long parentId() {
    if (protoPage.hasParentId()) {
      return protoPage.getParentId();
    }
    return 0;
  }

  @Override
  public boolean isRoot() {
    return !protoPage.hasParentId();
  }

  @Override
  public LongStream subpagesIds() {
    if (protoPage.getSubpagesIdsCount() == 0) {
      return LongStream.empty();
    }
    return protoPage.getSubpagesIdsList().stream().mapToLong(Number::longValue);
  }

  @Override
  public URI uri() {
    return uri;
  }

  private CharSequence content(SegmentType type) {
    switch (type) {
      case BODY:
        String subpagesContent =
            subpages().map(p -> p.content(SegmentType.BODY)).collect(Collectors.joining("\n"));
        if (subpagesContent.isEmpty()) {
          return content(SegmentType.SUB_BODY);
        }
        return CharSeqTools.concat(content(SegmentType.SUB_BODY), "\n", subpagesContent);
      case FULL_TITLE:
        Page p = this;
        CharSequence res = "";
        while (p.parent() != p) {
          res =
              CharSeqTools.concat(
                  p.parent().content(SegmentType.SECTION_TITLE), TITLE_DELIMETER, res);
          p = p.parent();
        }
        res = CharSeqTools.concat(res, content(SegmentType.SECTION_TITLE));
        return res;
      case SUB_BODY:
        return protoPage.getContent();
      case SECTION_TITLE:
        return protoPage.getTitle();
      default:
        return "";
    }
  }

  @Override
  public CharSequence content(SegmentType... types) {
    return Arrays.stream(types).map(this::content).collect(Collectors.joining("\n"));
  }

  @Override
  public List<CharSequence> categories() {
    return protoPage
        .getCategoriesList()
        .stream()
        .map(CharSequence.class::cast)
        .collect(Collectors.toList());
  }

  @Override
  public int incomingLinksCount(LinkType type) {
    switch (type) {
      case SECTION_LINKS:
        return protoPage.getIncomingLinksList().size();

      case ALL_LINKS:
        return root().incomingLinksCount(LinkType.SECTION_LINKS);

      default:
        return 0;
    }
  }

  @Override
  public int outgoingLinksCount(LinkType type) {
    switch (type) {
      case SECTION_LINKS:
        return protoPage.getOutgoingLinksList().size();

      case ALL_LINKS:
        return protoPage.getOutgoingLinksList().size()
            + subpages().mapToInt(page -> page.outgoingLinksCount(LinkType.ALL_LINKS)).sum();
      default:
        return 0;
    }
  }

  @Override
  public Stream<Link> outgoingLinks(LinkType type) {
    switch (type) {
      case SECTION_LINKS:
        return protoPage
            .getOutgoingLinksList()
            .stream()
            .map(l -> PlainLink.withSource(l, index, this));

      case ALL_LINKS:
        return Stream.concat(
            protoPage
                .getOutgoingLinksList()
                .stream()
                .map(l -> PlainLink.withSource(l, index, this)),
            subpages().flatMap(page -> page.outgoingLinks(LinkType.ALL_LINKS)));
      default:
        return Stream.empty();
    }
  }

  @Override
  public Stream<Link> incomingLinks(LinkType type) {
    switch (type) {
      case SECTION_LINKS:
        return protoPage
            .getIncomingLinksList()
            .stream()
            .map(l -> PlainLink.withTarget(l, index, this));

      case ALL_LINKS:
        return root().incomingLinks(LinkType.SECTION_LINKS);

      default:
        return Stream.empty();
    }
  }

  @Override
  public Page parent() {
    if (protoPage.hasParentId()) {
      return PlainPage.create(protoPage.getParentId(), this.index);
    }

    return this;
  }

  @Override
  public Page root() {
    Page cur = this;
    while (!cur.isRoot()) {
      cur = cur.parent();
    }
    return cur;
  }

  @Override
  public Stream<Page> subpages() {
    return this.subpagesIds().mapToObj(id -> PlainPage.create(id, this.index));
  }

  @Override
  public Stream<CharSequence> sentences(SegmentType type) {
    return index.sentences(content(type));
  }

  @Override
  public Vec titleVec() {
    if (titleVec == null) {
      synchronized (this) {
        if (titleVec == null) {
          titleVec = index.vecByTerms(index.parse(content(SegmentType.SECTION_TITLE)).collect(Collectors.toList()));
        }
      }
    }
    return titleVec;
  }

  @Override
  public Vec sentenceVec(CharSequence sentence) {
    if (sentenceVecs == null) {
      synchronized (this) {
        if (sentenceVecs == null) {
          sentenceVecs = sentences(SegmentType.BODY)
                  .collect(Collectors.toMap(Function.identity(), s -> index.vecByTerms(index.parse(s).collect(Collectors.toList())), (x, y) -> x));
        }
      }
    }
    return sentenceVecs.getOrDefault(sentence, Vec.EMPTY);
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof PlainPage) && (this == other || ((PlainPage) other).id == this.id);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.id);
  }

  static class PlainLink implements Page.Link {
    private final IndexUnits.Page.Link protoLink;
    private final IndexedPage sourcePage;
    private final IndexedPage targetPage;

    PlainLink(IndexUnits.Page.Link protoLink, IndexedPage sourcePage, IndexedPage targetPage) {
      this.protoLink = protoLink;
      this.sourcePage = sourcePage;
      this.targetPage = targetPage;
    }

    static PlainLink withTarget(
        IndexUnits.Page.Link protoLink, PlainIndex index, PlainPage targetPage) {
      return new PlainLink(
          protoLink, PlainPage.create(protoLink.getSourcePageId(), index), targetPage);
    }

    static PlainLink withSource(
        IndexUnits.Page.Link protoLink, PlainIndex index, PlainPage sourcePage) {
      return new PlainLink(
          protoLink,
          sourcePage,
          protoLink.hasTargetPageId()
              ? PlainPage.create(protoLink.getTargetPageId(), index)
              : PlainPage.EMPTY_PAGE);
    }

    static PlainLink fromProtoLinkOnly(IndexUnits.Page.Link protoLink, PlainIndex index) {
      return new PlainLink(
          protoLink,
          PlainPage.create(protoLink.getSourcePageId(), index),
          protoLink.hasTargetPageId()
              ? PlainPage.create(protoLink.getTargetPageId(), index)
              : PlainPage.EMPTY_PAGE);
    }

    @Override
    public boolean targetExists() {
      return targetPage != EMPTY_PAGE;
    }

    @Override
    public CharSequence targetTitle() {
      if (targetPage == EMPTY_PAGE) {
        return "";
      }
      return targetPage.content(SegmentType.SECTION_TITLE);
    }

    @Override
    public CharSequence text() {
      return protoLink.getText();
    }

    @Override
    public Page targetPage() {
      return targetPage;
    }

    @Override
    public Page sourcePage() {
      return sourcePage;
    }

    @Override
    public long position() {
      return protoLink.getPosition();
    }
  }
}
