package com.expleague.sensearch.web.suggest.hh_phrases;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.DispatcherType;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.web.HK2ToGuiceModule;
import com.expleague.sensearch.web.SearchEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class HHPhrases {
	
	public final static String text = "Мой дядя самых честных правил,\n" + 
			"Когда не в шутку занемог,\n" + 
			"Он уважать себя заставил\n" + 
			"И лучше выдумать не мог.\n" + 
			"Его пример другим наука;\n" + 
			"Но, боже мой, какая скука\n" + 
			"С больным сидеть и день и ночь,\n" + 
			"Не отходя ни шагу прочь!\n" + 
			"Какое низкое коварство\n" + 
			"Полуживого забавлять,\n" + 
			"Ему подушки поправлять,\n" + 
			"Печально подносить лекарство,\n" + 
			"Вздыхать и думать про себя:\n" + 
			"Когда же черт возьмет тебя!»\n" + 
			"II\n" + 
			"Так думал молодой повеса,\n" + 
			"Летя в пыли на почтовых,\n" + 
			"Всевышней волею Зевеса\n" + 
			"Наследник всех своих родных.\n" + 
			"Друзья Людмилы и Руслана!\n" + 
			"С героем моего романа\n" + 
			"Без предисловий, сей же час\n" + 
			"Позвольте познакомить вас:\n" + 
			"Онегин, добрый мой приятель,\n" + 
			"Родился на брегах Невы,\n" + 
			"Где, может быть, родились вы\n" + 
			"Или блистали, мой читатель;\n" + 
			"Там некогда гулял и я:\n" + 
			"Но вреден север для меня 1.\n" + 
			"III\n" + 
			"Служив отлично благородно,\n" + 
			"Долгами жил его отец,\n" + 
			"Давал три бала ежегодно\n" + 
			"И промотался наконец.\n" + 
			"Судьба Евгения хранила:\n" + 
			"Сперва Madame за ним ходила,\n" + 
			"Потом Monsieur ее сменил.\n" + 
			"Ребенок был резов, но мил.\n" + 
			"Monsieur l'Abbé, француз убогой,\n" + 
			"Чтоб не измучилось дитя,\n" + 
			"Учил его всему шутя,\n" + 
			"Не докучал моралью строгой,\n" + 
			"Слегка за шалости бранил\n" + 
			"И в Летний сад гулять водил.\n" + 
			"IV\n" + 
			"Когда же юности мятежной\n" + 
			"Пришла Евгению пора,\n" + 
			"Пора надежд и грусти нежной,\n" + 
			"Monsieur прогнали со двора.\n" + 
			"Вот мой Онегин на свободе;\n" + 
			"Острижен по последней моде,\n" + 
			"Как dandy 2 лондонский одет —\n" + 
			"И наконец увидел свет.\n" + 
			"Он по-французски совершенно\n" + 
			"Мог изъясняться и писал;\n" + 
			"Легко мазурку танцевал\n" + 
			"И кланялся непринужденно;\n" + 
			"Чего ж вам больше? Свет решил,\n" + 
			"Что он умен и очень мил.\n" + 
			"V\n" + 
			"Мы все учились понемногу\n" + 
			"Чему-нибудь и как-нибудь,\n" + 
			"Так воспитаньем, слава богу,\n" + 
			"У нас немудрено блеснуть.\n" + 
			"Онегин был по мненью многих\n" + 
			"(Судей решительных и строгих)\n" + 
			"Ученый малый, но педант:\n" + 
			"Имел он счастливый талант\n" + 
			"Без принужденья в разговоре\n" + 
			"Коснуться до всего слегка,\n" + 
			"С ученым видом знатока\n" + 
			"Хранить молчанье в важном споре\n" + 
			"И возбуждать улыбку дам\n" + 
			"Огнем нежданных эпиграмм.\n" + 
			"VI\n" + 
			"Латынь из моды вышла ныне:\n" + 
			"Так, если правду вам сказать,\n" + 
			"Он знал довольно по-латыне,\n" + 
			"Чтоб эпиграфы разбирать,\n" + 
			"Потолковать об Ювенале,\n" + 
			"В конце письма поставить vale,\n" + 
			"Да помнил, хоть не без греха,\n" + 
			"Из Энеиды два стиха.\n" + 
			"Он рыться не имел охоты\n" + 
			"В хронологической пыли\n" + 
			"Бытописания земли:\n" + 
			"Но дней минувших анекдоты\n" + 
			"От Ромула до наших дней\n" + 
			"Хранил он в памяти своей.\n" + 
			"VII\n" + 
			"Высокой страсти не имея\n" + 
			"Для звуков жизни не щадить,\n" + 
			"Не мог он ямба от хорея,\n" + 
			"Как мы ни бились, отличить.\n" + 
			"Бранил Гомера, Феокрита;\n" + 
			"Зато читал Адама Смита\n" + 
			"И был глубокой эконом,\n" + 
			"То есть умел судить о том,\n" + 
			"Как государство богатеет,\n" + 
			"И чем живет, и почему\n" + 
			"Не нужно золота ему,\n" + 
			"Когда простой продукт имеет.\n" + 
			"Отец понять его не мог\n" + 
			"И земли отдавал в залог.";
	
	public static double idf(Term t) {
		return 1;
	}
	
	public final Map<List<Term>, Double> map = new HashMap<>();
	static Term[] terms;
	
	final static double z = 0.75;
	
	public static void countSums(Map<List<Term>, Double> map, Term[] terms) {
		Set<Term> nearestForPosition = new HashSet<>();
		for (int i = 0; i < terms.length; i++) {
			Term t = terms[i];
			nearestForPosition.clear();
			for (int j = i - 1; j >= 0; j--) {
				if (nearestForPosition.add(terms[j])) {
					Term t1 = terms[j];
					List<Term> key = Arrays.asList(terms[i], terms[j]);
					map.putIfAbsent(key, 0.0);
					double oldval = map.get(key);
					oldval += idf(t1) / Math.pow(Math.abs(i - j), z) * 
							(t.equals(t1) ? 0.25 : 1.0);
					map.put(key, oldval);
				}
			}
			
			nearestForPosition.clear();
			for (int j = i + 1; j < terms.length; j++) {
				if (nearestForPosition.add(terms[j])) {
					Term t1 = terms[j];
					List<Term> key = Arrays.asList(terms[i], terms[j]);
					map.putIfAbsent(key, 0.0);
					double oldval = map.get(key);
					oldval += idf(t1) / Math.pow(Math.abs(i - j), z) * 
							(t.equals(t1) ? 0.25 : 1.0);
					map.put(key, oldval);
				}
			}
		}
	}
	
	public double qualifiedPhrase(Term[] phrase) {
		double res = 0;
		for (Term t : phrase) {
			for (Term t1 : phrase) {
				res += map.get(Arrays.asList(t, t1)) * idf(t);
			}
		}
		return Math.log(1 + res);
	}
	
	int incCounter(int[] cnt, int base) {
		int add = 1;
		for (int i = 0; i < cnt.length; i++) {
			cnt[i] += add;
			add = 0;
			if (cnt[i] == base) {
				cnt[i] = 0;
				add = 1;
			}
		}
		return add;
	}
	
	public Map<Double, Term[]> bestNGrams(int n, int limit) {
		int[] idxs = new int[n];
		Map<Double, Term[]> res = new TreeMap<>();
		while (incCounter(idxs, n) == 0) {
			Term[] val = new Term[n];
			for (int i = 0; i < n; i++) {
				val[i] = terms[idxs[i]];
			}
			res.put(qualifiedPhrase(val), val);
			res.keySet().removeIf(d -> res.size() > limit);
		}
		return res;
	}
	
	public void processPhrases(Term[] terms) {
		
		countSums(map, terms);
		bestNGrams(2, 10).forEach((d, tr) -> {
			for (Term t : tr) {
				System.out.print(tr + " ");
			}
			System.out.println(d);
		});
	}
	
	public static void main(String[] args) throws IOException {
		//Properties logProperties = new Properties();
	    //PropertyConfigurator.configure(logProperties);
		
		ObjectMapper objectMapper = new ObjectMapper();
	    Config config = objectMapper.readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);


	    Index index = new PlainIndex(config);

		
		terms = index.parse(text.toLowerCase())
				.map(t -> t.lemma())
				.toArray(Term[]::new);
		HHPhrases phrases = new HHPhrases();
		phrases.processPhrases(terms);
	}
}
