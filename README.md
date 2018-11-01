## Лёгкий поиск

У нас есть [GDrive][1] где можно найти архивы со статьями из Википедии,
 а так же **можно** добавлять свои файлы и библиотеки.
 
`Mini_Wiki.zip` - Архив с 10_000 статей, для быстрой проверки корректности.

`Mini_Wiki_categories.zip` - Архив с 60_503 статьями из следующих категорий: ```
      "Персоналии по алфавиту",
      "Животные",
      "Реки",
      "Музыкальные коллективы по алфавиту",
      "Компании по алфавиту",
      "Фильмы на английском языке",
      "Фильмы-драмы СССР",
      "Компании",
      "Населённые пункты",
      "Города",
      "Фильмы",
      ```
Кажется, что в дампе википедии есть некоторый баг, из-за которого не у всех статьей проставлены все категории (например, у статьи про Россию в дампе только две категории), поэтому есть некоторая вероятность того, что в этом архиве для каждой категории есть не все статьи. 
`wikiforia_dump_splitted.zip` - Полный архив. (Тестится минимум 4 часа)


### Запуск

Перед запуском приложения необходимо в файде `paths.json` изменить значение переменной `pathToZIP` на путь до архива с Википедией. 


### Ресурсы (папка resources)

Необходимо добавить в папку resources файл [mystem][2].

Добавьте в папку `resources` архив `vectors.txt.gz`, чтобы иметь возможность
работать с *Word Embeddings*. Если хотите убедиться, что всё сделали правильно, запустите тест `NearestFinderTest`. 

**P.s.** При необходимости используйте флаг `-Xmx<size>`, чтобы увеличить размер heap.

[1]: https://drive.google.com/drive/folders/1JGMrne_8oFg5V6bvbEb88nTbRJ830u1C?usp=sharing
[2]: https://tech.yandex.ru/mystem/
