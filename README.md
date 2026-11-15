# Лабораторная работа № 3 — работа с серверными классами

Реализованы общая часть со словарём и вариант 1: выдача презентации PowerPoint сервлетом и автоматическое открытие в Java-клиенте. Презентация включена в ресурсы сервера; её можно заменить своей.

## Что реализовано

- `server` — отдельный Java Web проект: HTML-форма, сервлет `MyServlet`, настройка `WEB-INF/web.xml`, база H2 через JDBC.
- `client` — отдельный Java-проект: Swing-приложение `ApacheClient`, обращающееся к сервлету по URL через Apache HttpClient, как в методичке. Доступа к базе у клиента нет.
- Русско-английский и англо-русский перевод, UTF-8 в обоих направлениях. Обрабатываются GET и POST через `processRequest`.
- База создаётся на сервере при первом запуске, таблица и исходные слова находятся в `server/src/main/resources/dictionary.sql`. Данные сохраняются в `data/dictionary.mv.db` внутри домена GlassFish. Файлы базы исключены из Git.
- `PresentationServlet` отдаёт файл `.pptx` по URL; кнопка Java-клиента скачивает и автоматически открывает его.
- Неизвестное слово возвращает HTTP 404, пустой запрос и неверное направление — HTTP 400. Для слова «мир» возвращаются оба перевода.

## Запуск в IntelliJ IDEA и GlassFish

Проект рассчитан на **GlassFish 7 (Jakarta EE 10)** и **JDK 21**. Сервер использует `jakarta.servlet`; регистрация сервлета остаётся в `web.xml`, без `@WebServlet`.

GlassFish установлен отдельно от репозитория в `~/GlassFish7-Lab3/glassfish7`. Для сервера используется JDK 21. На другом компьютере установите GlassFish 7 отдельно и замените путь к нему в командах ниже.

### Сборка в IDEA

1. Откройте корневой `pom.xml` как Maven-проект.
2. В **File → Project Structure → Project SDK** выберите **JDK 21**.
3. Откройте **View → Tool Windows → Maven** и нажмите **Reload All Maven Projects**. Если пункта Maven нет, нажмите правой кнопкой на корневой `pom.xml` → **Add as Maven Project**.
4. Запустите сохранённую конфигурацию **Build Application** либо `lab3 → Lifecycle → package` в окне Maven.
5. Результат сборки сервера — **`server/target/lab_servlet.war`**. Это архив приложения для установки на GlassFish.

### Запуск сервера и установка приложения (для любой редакции IDEA)

Встроенный терминал IDEA открывается через **View → Tool Windows → Terminal**. Выполните:

```sh
~/GlassFish7-Lab3/glassfish7/bin/asadmin start-domain domain1
```

Если сервер уже работает, повторно запускать его не нужно. Порт 8080 должен быть свободен от других серверов.

Откройте панель управления GlassFish: **http://localhost:4848**.

1. Выберите **Applications → Deploy**.
2. Выберите файл **`server/target/lab_servlet.war`**.
3. Тип — **Web Application**, имя приложения — **lab_servlet**, Context Root — **/lab_servlet**.
4. Нажмите **OK** и дождитесь окончания установки.

То же самое можно выполнить из терминала IDEA, находясь в корне репозитория:

```sh
~/GlassFish7-Lab3/glassfish7/bin/asadmin deploy --force=true --name lab_servlet --contextroot /lab_servlet server/target/lab_servlet.war
```

Откройте **http://127.0.0.1:8080/lab_servlet/**. Проверьте `кот → cat` и `book → книга`.

### Запуск Java-клиента

Не останавливая GlassFish, запустите **Dictionary Client** либо откройте `client/src/main/java/by/bsuir/client/ApacheClient.java` и нажмите зелёную стрелку у `main`. Если потребуется, выберите classpath модуля `dictionary-client`.

Адрес сервлета — `http://127.0.0.1:8080/lab_servlet/MyServlet`. При использовании другого порта укажите в **VM options** клиента `-Ddictionary.url=http://127.0.0.1:8081/lab_servlet/MyServlet`.

### Запуск прямо из конфигурации GlassFish в IDEA Ultimate

Интеграция GlassFish с окном Run требует Ultimate и плагина GlassFish. При его наличии можно создать **Run → Edit Configurations → + → GlassFish Server → Local**, выбрать сервер из `~/GlassFish7-Lab3/glassfish7`, JRE **21**, а во вкладке **Deployment** добавить собранный WAR через **External Source**. Если сервер уже запущен вручную, сначала остановите его командой ниже. Для повторной сборки и установки используйте Maven `package` и Redeploy.

Если пункта GlassFish Server нет, используйте запуск через терминал и панель управления, описанный выше: он не требует интеграции с IDEA.

### Обновление и остановка

После изменения Java, HTML или SQL **снова выполните `package` и переустановите WAR**: в панели Applications используйте Redeploy либо повторите команду `asadmin deploy --force=true ...` выше. Одного перезапуска сервера недостаточно, если в него установлен старый WAR.

Остановка сервера:

```sh
~/GlassFish7-Lab3/glassfish7/bin/asadmin stop-domain domain1
```

База по умолчанию хранится в **`~/GlassFish7-Lab3/glassfish7/glassfish/domains/domain1/data/dictionary.mv.db`**. Путь привязан к домену GlassFish и не зависит от рабочей папки IDEA. Для своей базы можно задать JVM-свойство сервера `dictionary.jdbc.url`. При первом запуске словарь наполняется 184 парами из SQL-файла.

HTML находится в корне webapp для прямого доступа из браузера. В `WEB-INF` находятся `web.xml` с регистрацией сервлета и `glassfish-web.xml` с путём приложения и кодировкой UTF-8.

## Как устроен запрос

Форма отправляет `GET /lab_servlet/MyServlet?txt=кот&direction=ru-en`. `web.xml` связывает `/MyServlet` с классом `by.bsuir.server.MyServlet`. `doGet` / `doPost` вызывают `processRequest`; он читает параметры `HttpServletRequest`, запрашивает базу через `PreparedStatement` и записывает ответ в `HttpServletResponse` через `PrintWriter`.

Направления: `ru-en`, `en-ru`. Для Java-клиента добавляется `format=text`, и сервлет возвращает текст вместо HTML. HTTP-запрос выполняется в `SwingWorker`, чтобы окно не зависало во время ожидания сервера.

## Вариант 1 — презентация PowerPoint

Задание: написать сервлет, возвращающий клиенту презентацию PowerPoint с автоматическим открытием на стороне клиента.

### Как добавить свою презентацию

1. В IDEA откройте `server/src/main/resources/presentations`.
2. Скопируйте туда свою готовую презентацию и назовите её **`presentation.pptx`**. Если у вас старый файл `.ppt`, сначала сохраните его в PowerPoint в формате `.pptx`: простого переименования расширения недостаточно.
3. Выполните **Build Application** (Maven `package`).
4. Выполните **Redeploy** в GlassFish либо команду развёртывания из инструкции выше.
5. Запустите заново Java-клиент и на вкладке **«Презентация»** нажмите **«Открыть презентацию»**.

При замене презентации повторите сборку и Redeploy. Папка ресурсов находится на сервере; Java-клиент получает файл по HTTP и не читает его из проекта напрямую.

### Как работает открытие

`PresentationServlet` зарегистрирован в `web.xml` по пути `/PresentationServlet`. Он читает ресурс `/presentations/presentation.pptx` и передаёт его байты через `response.getOutputStream()`. Заголовок `Content-Type` указывает формат PowerPoint, а `Content-Disposition` — имя файла для скачивания. Если файл пока не добавлен, сервер возвращает HTTP 404 с пояснением; словарь продолжает работать.

`PresentationClient` отправляет HTTP-запрос через Apache HttpClient, проверяет ответ и сохраняет файл во временную папку системы. Затем вызывает `Desktop.getDesktop().open(...)`: система открывает файл в приложении, назначенном для `.pptx`. Чтобы открывался именно Microsoft PowerPoint, назначьте его приложением по умолчанию для этого расширения. На macOS: Finder → свойства любого `.pptx` → «Открывать в программе» → Microsoft PowerPoint → «Настроить… / Изменить всё» (название зависит от версии macOS).

Скачивание и открытие выполняются в `SwingWorker`, чтобы окно не зависало. Если скачивание успешно, но открыть файл не удалось, клиент показывает путь к сохранённому файлу. Временный файл не удаляется при выходе из клиента, поскольку PowerPoint может продолжать с ним работать; очистка временной папки выполняется отдельно.

На сайте есть только ссылка **«Скачать презентацию (.pptx)»**. Сервлет отдаёт файл с правильным типом, размером и заголовком `Content-Disposition: attachment`. После скачивания пользователь может открыть его вручную.

Автоматическое открытие реализовано только в Java-клиенте через `Desktop.open`. Сайт не вызывает PowerPoint и не требует изменения настроек браузера.

Адрес по умолчанию: `http://127.0.0.1:8080/lab_servlet/PresentationServlet`. Он автоматически использует адрес сервера из `dictionary.url`; при необходимости можно отдельно указать VM option клиента `-Dpresentation.url=http://127.0.0.1:8080/lab_servlet/PresentationServlet`.

## Добавление слов

Откройте `server/src/main/resources/dictionary.sql` в IDEA. В конце списка замените последнюю точку с запятой на запятую и добавьте новые пары:

```sql
('новое русское слово', 'english translation'),
('ещё одно слово', 'another translation');
```

Сначала русский текст, затем английский. Пишите строчными буквами без пробелов по краям. Между парами ставьте запятую, после последней — точку с запятой. Апостроф внутри слова удваивается: `'it''s'`.

Выполните Maven **package** и **Redeploy** приложения в GlassFish: обновлённый SQL-файл попадёт в WAR и выполнится при инициализации сервлета. Новые слова появятся и на сайте, и в приложении; клиент перезапускать не нужно. Каждая пара сразу работает в обоих направлениях.

`MERGE INTO` не дублирует существующие пары. Несколько переводов одного слова добавляйте отдельными парами, как у слова «мир». Удаление или изменение пары в SQL-файле не удаляет старую пару из уже созданной базы: для удаления нужна отдельная SQL-команда `DELETE`.


Документация: [GlassFish 7](https://glassfish.org/download_gf7.html), [настройка GlassFish в IDEA](https://www.jetbrains.com/help/idea/run-debug-configuration-glassfish-server.html), [H2 JDBC](https://h2database.github.io/html/quickstart.html).
