## Лёгкий поиск

[Тект статьи][5]

[![Build Status](https://travis-ci.org/solariq/sensearch.svg?branch=master)](https://travis-ci.org/solariq/sensearch)

У нас есть [GDrive][1] где можно найти архивы со статьями из Википедии,
 а так же **можно** добавлять свои файлы и библиотеки.
 
`Mini_Wiki.zip` - Архив с небольшим количеством статей, для тестов и экспериментов по темам:
      
     "Актёры России"
     "Поэты России"
     "Правители"

`Mini_Wiki_categories.zip` - Архив с 60k+ статьями из следующих категорий: 

     "Актёры России"
     "Футболисты России"
     "Писатели России по алфавиту"
     "Поэты России"
     "Актрисы России"
     "Мастера спорта России международного класса"
     "Правители"
     и что-то про города
      
Кажется, что в дампе википедии есть некоторый баг, из-за которого не у всех статьей проставлены все категории (например, у статьи про Россию в дампе только две категории), поэтому есть некоторая вероятность того, что в этом архиве для каждой категории есть не все статьи. 

`wikiforia_dump_splitted.zip` - Полный архив. (без категорий)

### Запуск

Для того чтобы запустить фронтенд, необходимо сделать следующее:
```
sudo apt-get install npm
sudo apt-get install nodejs
```

Затем нужно проверить что поставилась `node` нужной (8+) версии:
```
node -v
```
Если вам не повезло и поставилась старая версия (например, на Ubuntu 16.04 ставится 4 версия), 
то качаем [nodejs][3].

Дальше нужно сделать:
```
sudo npm install -g @angular/cli
cd webapp
npm install
npm install --save @angular/material @angular/cdk @angular/animations
```

Теперь у нас поставлено все необходимое, дальше необходимо все это собрать, запустив это из папки `webapp`:
```
ng build
```
После чего можно запускать сервак и все будет работать.

Для запуска сервера может понадобиться индекс. Для того, чтобы при запуске сервера строился индекс, нужно в конфиге установить флаг buildIndexFlag в true, и запустить сервер. 

Если будут выпадать ошибки вида `Cannot find module <module-name>`, 
то необходимо пставить и эти модули
```
npm install --save <module-name>
```
**Повторять до успеха**

**Замечание**: если вдруг что-то поменялось на фронтенде, то для того, чтобы эти изменения вступили в силу, нужно запустить `ng build`.

Перед запуском приложения необходимо в файде `config.json` изменить значение переменной `pathToZIP` на путь до архива с Википедией. 


### Ресурсы (папка resources)

Необходимо добавить в папку resources файл [mystem][2].

Добавьте в папку `resources` архив `vectors.txt.gz`, чтобы иметь возможность
работать с *Word Embeddings*. Если хотите убедиться, что всё сделали правильно, запустите тест `NearestFinderTest`. 

`vectors50.txt.gz` - векторы размерности 50 (~800 Мб вашей оперативки)

**P.s.** При необходимости используйте флаг `-Xmx<size>`, чтобы увеличить размер heap.

### Метрика

В проекте существует метрика, которая в данный момент оценивает ранжировку относительно Google.
При каждом запросе автоматически выводится DCG запроса в консоль, а так же в папку `resources/Metrics/<request_name>` складывается файл со значением DCG.

Так же есть классы `RebaseMetrics` и `MetricTest` для оценки измениений ранжировки относительно статических данных от Google и запросов.

__Инструкция по применению__
>Если вы уже запускали `RebaseMetrics`, то можете сразу перейти к пункту 2.

1. Запустить `RebaseMetrics`, после чего в папке `resources/Metrics/` появятся текущие результаты выдачи Google на популярные запросы в Википедию по нашим категориям. Так же необоходимо не забыть остановить исполнение данного класса.
2. Запустить `MetricTest`, после чего на кансоли появится значения старой метрики и новой по всем запросам из файла `resources/Queries.txt`.
3. Теперь можно оценивать прогресс.

### Формула
Чтобы потренировать формулу нужны следующие вещи: 
* pool, который собирается `com.expleague.sensearch.miner.pool.<*>PoolBuilder`
* jmll в виде jar, который можно получить с помощью команды (запущеной в руте jmll):  
`mvn -DskipTests=true package -P build-ml-jar,!publish`  результат работы этой штуки будет лежать в `ml/target/ml.jar`
Фомула тренируется следующей командой:
`java -classpath <sensearch-dir>/target/classes:<jmll-path>/ml/target/ml.jar com.expleague.ml.JMLLCLI fit --print-period 10  --json-format --learn <pool-name.pool> -X 1/0.7 -v -O 'GradientBoosting(local=SatL2, weak=GreedyObliviousTree(depth=6), step=0.01, iterations=600)'`
В результате этой мумба-юмбы получатся 2 файлика: `<pool-name.pool>.model` и `<pool-name.pool>.grid`. Чтобы поиск начал работать
с новой формулой, нужно первый из низ (который .model) положить в `src/main/resources/models/ranking.model`.

Если вы хотите посмотреть насколько тот или иной фактор важен для формулы, запускаем другую магическую команду:
`java -classpath <sensearch-dir>/target/classes:<jmll-path>/ml/target/ml.jar com.expleague.ml.JMLLCLI interpret --model <pool-name.pool>.model --grid <pool-name.pool>.grid --json-format --learn <pool-name.pool> -I histogram`
В результате будет сгенерирована табличка состоящая из таких элементов `bin:x-val:y-val`, читать ее надо так: при изменении x-val от одного элемента до другого приведет к разнице в значении формулы, соответствующего разнице y-val.
 

[1]: https://drive.google.com/drive/folders/1JGMrne_8oFg5V6bvbEb88nTbRJ830u1C?usp=sharing
[2]: https://tech.yandex.ru/mystem/
[3]: http://nodejs.org
[4]: https://github.com/solariq/sensearch/blob/master/src/main/java/com/expleague/sensearch/CrawlWordstatData.java
[5]: https://www.papeeria.com/join?token_id=ed389d64-b87c-40ba-8c6b-0cee02d2c66d&retry=3
