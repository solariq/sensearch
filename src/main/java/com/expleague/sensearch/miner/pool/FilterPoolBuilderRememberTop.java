package com.expleague.sensearch.miner.pool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.Trans;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Annotations.RankFilterModel;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.filter.FilterFeatures;
import com.expleague.sensearch.features.sets.filter.TargetFeatureSet;
import com.expleague.sensearch.filter.FilterMinerPhase;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class FilterPoolBuilderRememberTop {
	private static final int FILTER_SIZE = 100;
	private final Index index;
	private final static FeatureMeta TITLE = FeatureMeta
			.create("dist-title", "cos distance between Query and Title", FeatureMeta.ValueType.VEC);
	private final static FeatureMeta SECTION = FeatureMeta
			.create("dist-section", "cos distance between Query and Nearest Section Body", FeatureMeta.ValueType.VEC);
	private final static FeatureMeta LINK = FeatureMeta
			.create("dist-link", "cos distance between Query and Nearest Incoming Link", FeatureMeta.ValueType.VEC);

	private final Trans model;

	private final ObjectMapper mapper = new ObjectMapper();
	
	private final Path rememberDir = Paths.get("pbdata/filterPhaseTop/");
	
	private File getRememberTopFile(String queryString) {
		return rememberDir.resolve("query_" + queryString).toFile();
	}
	
	@Inject
	public FilterPoolBuilderRememberTop(Index index, 
			@RankFilterModel Pair<Function, FeatureMeta[]> rankModel) {
		this.index = index;
		this.model = (Trans) rankModel.getFirst();
	}

	public static void main(String[] args) throws IOException {
		Injector injector = Guice.createInjector(new AppModule());
		injector.getInstance(FilterPoolBuilderRememberTop.class).build(Paths.get("filter.pool"));
	}

	private  List<URI> getSavedQueryTop(String queryString) throws IOException {
		List<URI> res = new ArrayList<>();
		try {
			res = mapper.readValue(
					getRememberTopFile(queryString),
					new TypeReference<List<URI>>(){});
		} catch (FileNotFoundException fnfe) {}
		return res;
	}
	
	private  void saveQueryTop(String queryString, List<URI> l) throws IOException {
		Files.createDirectories(rememberDir);
		mapper.writeValue(
						getRememberTopFile(queryString),
						l);
	}

	public void build(Path poolPath) {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("./wordstat/queries.txt"))) {
			FastRandom rand = new FastRandom();
			DataSetMeta meta =
					new JsonDataSetMeta(
							"Google", "sensearch", new Date(), QURLItem.class, rand.nextBase64String(32));
			List<String> queries = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				queries.add(line);
			}
			FilterFeatures features = new FilterFeatures();
			TargetFeatureSet targetFeatureSet = new TargetFeatureSet();

			Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, targetFeatureSet);

			int status = 0;
			for (String queryString : queries) {
				if (status % 100 == 0) {
					System.err.println(status + " queries completed");
				}
				if (Files.exists(Paths.get("./wordstat").resolve("query_" + queryString))) {
					status++;
					BufferedReader readerFile = Files.newBufferedReader(Paths.get("./wordstat").resolve("query_" + queryString));

					Query query = BaseQuery.create(queryString, index);
					ObjectMapper objectMapper = new ObjectMapper();

					Set<CharSeq> googleTitles = Arrays.stream(objectMapper.readValue(readerFile, ResultItemImpl[].class))
							.map(ResultItemImpl::title)
							.map(CharSeq::create)
							.collect(Collectors.toSet());

					Map<Page, Features> allDocs = index.fetchDocuments(query, FilterMinerPhase.FILTERED_DOC_NUMBER);
					
					List<URI> rememberedURIs = getSavedQueryTop(queryString);
					
					
					rememberedURIs.forEach(uri -> {
						Page page = index.page(uri);
						accept(poolBuilder,
								page,
								query,
								((PlainIndex)index).filterFeatures(query, uri)
								);
						allDocs.remove(page);
					});


					final int[] cnt = {0};
					allDocs
					.forEach((page, feat) -> {
						if (googleTitles.contains(CharSeq.create(page.content(SegmentType.SECTION_TITLE)))) {
							accept(poolBuilder, page, query, feat);
						} else if (cnt[0] < FILTER_SIZE) {
							accept(poolBuilder, page, query, feat);
							cnt[0]++;
						}
					});
					
					allDocs.entrySet().stream()
					.sorted(
							Comparator.comparingDouble(
									e -> 
							-model.trans(e.getValue().features()).get(0))
					)
					.limit(10)
					.forEach(e -> {
						rememberedURIs.add(e.getKey().uri());
					});
					
					saveQueryTop(queryString, rememberedURIs);
				}
			}

			Pool<QURLItem> pool = poolBuilder.create();
			DataTools.writePoolTo(pool, Files.newBufferedWriter(poolPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void accept (Pool.Builder<QURLItem> poolBuilder, Page page, Query query, Features feat) {
		poolBuilder.accept(new QURLItem(page, query));
		poolBuilder.features().map(Functions.cast(FilterFeatures.class))
		.filter(Objects::nonNull)
		.forEach(fs -> {
			fs.withBody(feat.features(SECTION).get(0));
			fs.withLink(feat.features(LINK).get(0));
			fs.withTitle(feat.features(TITLE).get(0));
		});
		poolBuilder.advance();
	}
}
