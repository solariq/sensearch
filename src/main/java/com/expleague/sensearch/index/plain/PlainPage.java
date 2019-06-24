package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.util.cache.CacheStrategy.Type;
import com.expleague.commons.util.cache.impl.FixedSizeCache;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.utils.TokenParser;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
        public Stream<Term> content(boolean punct, SegmentType... types) {
          return Stream.empty();
        }

        @Override
        public CharSequence rawContent(SegmentType... types) {
          return "";
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
        public Stream<List<Term>> sentences(boolean punct, SegmentType t) {
          return Stream.empty();
        }

        @Override
        public Vec titleVec() {
          return Vec.EMPTY;
        }

        @Override
        public Stream<Vec> sentenceVecs() {
          return Stream.empty();
        }
      };

  private static final Logger LOG = LoggerFactory.getLogger(PlainPage.class);

  private final long id;
  private final PlainIndex index;
  private final IndexUnits.Page protoPage;
  private final URI uri;
  private final boolean isEmpty;
  private volatile Vec titleVec;
  private volatile Vec[] sentenceVecs;

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

  private Term tokenIdToTerm(int tokenId) {
    return index.term(TokenParser.toId(tokenId));
  }

  private IntStream intContent(SegmentType type, boolean punct) {
    switch (type) {
      case SECTION_TITLE:
        if (punct) {
          return protoPage.getTitle().getTokenIdsList().stream().mapToInt(i -> i);
        } else {
          return protoPage.getTitle().getTokenIdsList().stream().mapToInt(i -> i).filter(TokenParser::isWord);
        }
      case SUB_BODY:
        if (punct) {
          return protoPage.getContent().getTokenIdsList().stream().mapToInt(i -> i);
        } else {
          return protoPage.getContent().getTokenIdsList().stream().mapToInt(i -> i).filter(TokenParser::isWord);
        }
      case BODY:
        IntStream subpagesContent = subpages()
            .flatMapToInt(p -> ((PlainPage) p).intContent(type, punct));
        return IntStream.concat(intContent(SegmentType.SUB_BODY, punct), subpagesContent);
      case FULL_TITLE:
        Page p = this;
        IntStream res = intContent(SegmentType.SECTION_TITLE, punct);
        while (p.parent() != p) {
          res = IntStream
              .concat(((PlainPage) p.parent()).intContent(SegmentType.SECTION_TITLE, punct), res);
          p = p.parent();
        }
        return res;
      default:
        return IntStream.empty();
    }
  }

  private Stream<Term> content(SegmentType type, boolean punct) {
    return intContent(type, punct).mapToObj(this::tokenIdToTerm);
  }

  @Override
  public Stream<Term> content(boolean punct, SegmentType... types) {
    return Arrays.stream(types).flatMap(t -> content(t, punct));
  }

  @Override
  public CharSequence rawContent(SegmentType... types) {
    return content(true, types).map(Term::text).collect(Collectors.joining(" "));
  }

  //TODO: still not parse categories(((
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
  public Stream<List<Term>> sentences(boolean punct, SegmentType type) {
    return index.sentences(intContent(type, true))
        .map(terms -> terms.stream().filter(t -> !t.isPunctuation()).collect(Collectors.toList()));
  }

  @Override
  public Vec titleVec() {
    if (titleVec == null) {
      synchronized (this) {
        if (titleVec == null) {
          titleVec = index
              .vecByTerms(content(false, SegmentType.SECTION_TITLE).collect(Collectors.toList()));
        }
      }
    }
    return titleVec;
  }

  @Override
  public Stream<Vec> sentenceVecs() {
    if (sentenceVecs == null) {
      synchronized (this) {
        if (sentenceVecs == null) {
          sentenceVecs = sentences(false, SegmentType.BODY)
              .map(index::vecByTerms)
              .toArray(Vec[]::new);
        }
      }
    }
    return Arrays.stream(sentenceVecs);
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
    public Stream<Term> targetTitle() {
      if (targetPage == EMPTY_PAGE) {
        return Stream.empty();
      }
      return targetPage.content(false, SegmentType.SECTION_TITLE);
    }

    @Override
    public Stream<Term> text() {
      PlainPage page = (PlainPage) sourcePage;
      return protoLink.getText().getTokenIdsList().stream()
          .mapToInt(n -> n)
          .filter(TokenParser::isWord)
          .mapToObj(page::tokenIdToTerm);
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
