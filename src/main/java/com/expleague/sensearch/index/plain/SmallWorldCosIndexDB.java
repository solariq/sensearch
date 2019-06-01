package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
import com.expleague.commons.math.vectors.impl.nn.impl.EntryImpl;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.Pair;
import com.expleague.sensearch.core.ByteTools;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.WriteBatch;

public class SmallWorldCosIndexDB implements NearestNeighbourIndex {

  private static final long META_ID = Long.MAX_VALUE;
  private static final int BATCH_SIZE = 256;

  private final FastRandom random;
  private final int dim;
  private final int maxNeighbours;
  private final int numSearches;

  private final Embedding embedding;

  private final DB smallWorldDb;

  private final TLongList ids;
  private final TLongObjectMap<TLongList> graph;

  public SmallWorldCosIndexDB(
      FastRandom random,
      int dim,
      int maxNeighbours,
      int numSearches,
      Embedding embedding,
      DB smallWorldDb) {
    this.random = random;
    this.dim = dim;
    this.maxNeighbours = maxNeighbours;
    this.numSearches = numSearches;

    this.embedding = embedding;
    this.smallWorldDb = smallWorldDb;

    graph = new TLongObjectHashMap<>();
    ids = new TLongArrayList();

    DBIterator iterator = smallWorldDb.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining((entry) -> {
      final long id = Longs.fromByteArray(entry.getKey());
      final long[] neighbours = ByteTools.toLongArray(entry.getValue());
      graph.put(id, new TLongArrayList(neighbours));
      ids.add(id);
    });
  }

  private SmallWorldCosIndexDB(
      int dim,
      int maxNeighbours,
      int numSearches,
      Embedding embedding,
      DB smallWorldDb,
      TLongList ids,
      TLongObjectMap<TLongList> graph) {
    this.random = new FastRandom();
    this.dim = dim;
    this.maxNeighbours = maxNeighbours;
    this.numSearches = numSearches;
    this.embedding = embedding;
    this.smallWorldDb = smallWorldDb;
    this.ids = ids;
    this.graph = graph;
  }

  static SmallWorldCosIndexDB load(DB smallWorldDb, Embedding embedding) {
    int[] meta = ByteTools.toIntArray(smallWorldDb.get(Longs.toByteArray(META_ID)));
    int dim = meta[0];
    int maxNeighbours = meta[1];
    int numSearches = meta[2];

    final TLongObjectMap<TLongList> graph = new TLongObjectHashMap<>();
    final TLongList ids = new TLongArrayList();

    DBIterator iterator = smallWorldDb.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining(entry -> {
      final long id = Longs.fromByteArray(entry.getKey());
      if (id == META_ID) {
        return;
      }
      final long[] edges = ByteTools.toLongArray(entry.getValue());
      graph.put(id, new TLongArrayList(edges));
      ids.add(id);
    });

    return new SmallWorldCosIndexDB(dim, maxNeighbours, numSearches, embedding, smallWorldDb, ids, graph);
  }

  @Override
  public int dim() {
    return dim;
  }

  @Override
  public Stream<Entry> nearest(Vec query) {
    return StreamSupport
        .stream(
            Spliterators
                .spliteratorUnknownSize(new SmallWorldIterator(query), Spliterator.ORDERED | Spliterator.SORTED),
            false);
  }

  public Stream<Entry> nearest(Vec query, int max) {
    return StreamSupport
        .stream(
            Spliterators
                .spliteratorUnknownSize(new SmallWorldIterator(query, max), Spliterator.ORDERED | Spliterator.SORTED),
            false);
  }


  public void appendBatch(long[] addIds, Vec[] vecs) {
    long start = System.nanoTime();
    int size = addIds.length;
    if (ids.isEmpty()) {
      append(addIds[size - 1], vecs[size - 1]);
      size--;
    }
    IntStream.range(0, size).parallel()
        .mapToObj(
            i -> Pair
                .create(addIds[i], nearest(vecs[i], maxNeighbours).limit(maxNeighbours).collect(Collectors.toList())))
        .collect(Collectors.toList())
        .forEach(pair -> {
          final long id = pair.first;
          pair.second.forEach(entry -> {
            final long neighbourId = entry.id();
            graph.putIfAbsent(id, new TLongArrayList());
            graph.putIfAbsent(neighbourId, new TLongArrayList());
            graph.get(id).add(neighbourId);
            graph.get(neighbourId).add(id);
          });
          ids.add(id);
        });
    if (random.nextInt(100) == 0) {
      System.out.println("db size = " + ids.size() + ", append took " + (System.nanoTime() - start) / 1e6 + " ms");
    }
  }

  @Override
  public void append(long id, Vec vec) {
    if (ids.isEmpty()) {
      ids.add(id);
      graph.put(id, new TLongArrayList());
      return;
    }

    long start = System.nanoTime();
    nearest(vec, maxNeighbours).limit(maxNeighbours).forEach(entry -> {
      final long neighbourId = entry.id();
      graph.putIfAbsent(id, new TLongArrayList());
      graph.putIfAbsent(neighbourId, new TLongArrayList());
      graph.get(id).add(neighbourId);
      graph.get(neighbourId).add(id);
    });
    if (random.nextInt(100) == 0) {
      System.out.println("db size = " + ids.size() + ", append took " + (System.nanoTime() - start) / 1e6 + " ms");
    }
    ids.add(id);
  }

  @Override
  public void remove(long id) {
    throw new UnsupportedOperationException();
  }

  public void save() {
    smallWorldDb.put(Longs.toByteArray(META_ID), ByteTools.toBytes(new int[]{dim, maxNeighbours, numSearches}));

    WriteBatch[] writeBatch = {smallWorldDb.createWriteBatch()};
    int[] batchSize = {0};

    graph.forEachEntry((id, edges) -> {
      writeBatch[0].put(Longs.toByteArray(id), ByteTools.toBytes(edges.toArray()));
      batchSize[0]++;
      if (batchSize[0] >= BATCH_SIZE) {
        smallWorldDb.write(writeBatch[0]);
        try {
          writeBatch[0].close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        writeBatch[0] = smallWorldDb.createWriteBatch();
      }
      return true;
    });

    smallWorldDb.write(writeBatch[0]);
  }

  private double distance(long id, Vec vec) {
    final Vec idVec = embedding.vec(id);
    return (1 - VecTools.multiply(idVec, vec)) / 2;
  }


  private class SmallWorldIterator implements Iterator<Entry> {

    final TLongSet visited = new TLongHashSet();
    final TLongDoubleMap distances = new TLongDoubleHashMap();
    final TreeSet<Long> heap = new TreeSet<>(Comparator.comparing(distances::get));
    final TreeSet<Long> futureCandidates = new TreeSet<>(Comparator.comparing(distances::get));

    final Vec query;
    final int max;

    double curOptDist = Double.MAX_VALUE;
    long curOptId = Long.MAX_VALUE;

    SmallWorldIterator(Vec query) {
      this(query, Integer.MAX_VALUE);
    }

    public SmallWorldIterator(Vec query, int max) {
      this.max = max;

      query = VecTools.copy(query);
      VecTools.scale(query, 1.0 / VecTools.norm(query));
      this.query = query;

      for (int i = 0; i < Math.min(ids.size(), numSearches); i++) {
        long id = ids.get(random.nextInt(ids.size()));
        while (heap.contains(id)) {
          id = ids.get(random.nextInt(ids.size()));
        }

        addCandidate(id, distance(id, query));
      }

    }

    private void addCandidate(long id, double d) {
      visited.add(id);
      distances.put(id, d);
      if (curOptDist > d) {
        if (curOptId != Long.MAX_VALUE) {
          futureCandidates.add(curOptId);
        }

        curOptDist = d;
        curOptId = id;
      } else {
        futureCandidates.add(id);
      }

      if (futureCandidates.size() > max) {
        futureCandidates.pollLast();
      }
      heap.add(id);
      if (heap.size() > max) {
        heap.pollLast();
      }
    }

    @Override
    public boolean hasNext() {
      return heap.size() > 0 || !futureCandidates.isEmpty() || curOptId != Long.MAX_VALUE;
    }

    @Override
    public Entry next() {
      while (heap.size() > 0) {
        final long curId = Objects.requireNonNull(heap.pollFirst());
        final double oldOptDist = curOptDist;

        graph.get(curId).forEach(nextId -> {
          if (visited.contains(nextId)) {
            return true;
          }

          addCandidate(nextId, distance(nextId, query));
          return true;
        });

        if (oldOptDist < curOptDist + 1e-8 && !futureCandidates.isEmpty() && curOptId != Long.MAX_VALUE) {
          Entry result = new EntryImpl(0, curOptId, embedding.vec(curOptId), curOptDist);

          curOptId = Objects.requireNonNull(futureCandidates.pollFirst());
          curOptDist = distances.get(curOptId);

//          if (random.nextInt(2000) == 0) {
//            System.out.println(1.0 * visited.size() / ids.size());
//          }
          return result;
        }
      }

      if (curOptId == Long.MAX_VALUE && !futureCandidates.isEmpty()) {
        curOptId = Objects.requireNonNull(futureCandidates.pollFirst());
        curOptDist = distances.get(curOptId);
      }

      Entry result = new EntryImpl(0, curOptId, embedding.vec(curOptId), curOptDist);
      if (!futureCandidates.isEmpty()) {
        curOptId = Objects.requireNonNull(futureCandidates.pollFirst());
        curOptDist = distances.get(curOptId);
      } else {
        curOptId = Long.MAX_VALUE;
      }

      if (random.nextInt(2000) == 0) {
        System.out.println("Visited percentage: " + 1.0 * visited.size() / ids.size());
      }
      return result;
    }
  }

}
