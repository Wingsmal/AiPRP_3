package by.bsuir.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ApacheClient {
    static final String URL = System.getProperty("dictionary.url",
            "http://127.0.0.1:8080/lab_servlet/MyServlet");

    public static String translate(String word, String direction) throws Exception {
        var uri = new URIBuilder(URL).setCharset(StandardCharsets.UTF_8)
                .addParameter("txt", word).addParameter("direction", direction)
                .addParameter("format", "text").build();
        var config = RequestConfig.custom().setConnectTimeout(5000)
                .setSocketTimeout(5000).setConnectionRequestTimeout(5000).build();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
             CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (response.getStatusLine().getStatusCode() != 200) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 404) throw new java.io.IOException("Этого слова пока нет в словаре. Попробуйте другое.");
                if (status == 400 && response.getEntity().getContentType() != null
                        && response.getEntity().getContentType().getValue().startsWith("text/plain")) {
                    throw new java.io.IOException(body);
                }
                throw new java.io.IOException("Сервер не смог выполнить перевод (HTTP " + status + "). Попробуйте позже.");
            }
            return body;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* Use the standard Swing appearance if unavailable. */ }
            showWindow();
        });
    }

    private static void showWindow() {
        JFrame frame = new JFrame("Русско-английский словарь (Презентация и Котировки)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel content = createContent();
        frame.setContentPane(content);
        JButton primary = (JButton) content.getClientProperty("translateButton");
        frame.getRootPane().setDefaultButton(primary);
        setPrimaryText(primary);
        frame.setMinimumSize(new Dimension(520, 420));
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        ((JTextField) content.getClientProperty("wordField")).requestFocusInWindow();
    }

    static JPanel createContent() {
        Color muted = UIManager.getColor("Label.foreground");
        Color error = new Color(166, 42, 42);
        JComboBox<String> direction = new JComboBox<>(new String[]{"Русский → английский", "Английский → русский"});
        JTextField word = new JTextField(24);
        word.setFont(word.getFont().deriveFont(16f));
        word.setToolTipText("Введите слово на выбранном языке");
        JButton translate = new JButton("Перевести");
        JTextArea result = new JTextArea(4, 28);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setFont(word.getFont().deriveFont(18f));
        result.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane resultScroll = new JScrollPane(result);
        resultScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JButton copy = new JButton("Скопировать");
        copy.setEnabled(false);
        copy.addActionListener(event -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(result.getText()), null);
        });
        JTextArea status = note("Например: кот, книга, мир. Для перевода можно нажать Enter.", 2);
        JLabel languageLabel = new JLabel("Направление");
        languageLabel.setLabelFor(direction);
        JPanel languageRow = new JPanel(new BorderLayout(12, 0));
        languageRow.add(languageLabel, BorderLayout.WEST);
        languageRow.add(direction, BorderLayout.CENTER);
        JLabel wordLabel = new JLabel("Слово");
        wordLabel.setLabelFor(word);
        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.add(word, BorderLayout.CENTER);
        inputRow.add(translate, BorderLayout.EAST);
        JPanel input = new JPanel(new BorderLayout(0, 6));
        input.add(wordLabel, BorderLayout.NORTH);
        input.add(inputRow, BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(0, 16));
        controls.add(languageRow, BorderLayout.NORTH);
        controls.add(input, BorderLayout.CENTER);
        JLabel resultLabel = new JLabel("Перевод");
        resultLabel.setLabelFor(result);
        JPanel resultHeader = new JPanel(new BorderLayout());
        resultHeader.add(resultLabel, BorderLayout.WEST);
        resultHeader.add(copy, BorderLayout.EAST);
        JPanel output = new JPanel(new BorderLayout(0, 6));
        output.add(resultHeader, BorderLayout.NORTH);
        output.add(resultScroll, BorderLayout.CENTER);
        JPanel dictionary = new JPanel(new BorderLayout(0, 14));
        dictionary.setBorder(BorderFactory.createEmptyBorder(20, 20, 14, 20));
        dictionary.add(controls, BorderLayout.NORTH);
        dictionary.add(output, BorderLayout.CENTER);
        dictionary.add(status, BorderLayout.SOUTH);

        translate.addActionListener(event -> {
            String inputWord = word.getText().strip();
            result.setText("");
            copy.setEnabled(false);
            if (inputWord.isEmpty() || inputWord.length() > 100) {
                status.setForeground(error);
                status.setText("Введите слово длиной от 1 до 100 символов.");
                word.requestFocusInWindow();
                return;
            }
            String selected = direction.getSelectedIndex() == 0 ? "ru-en" : "en-ru";
            translate.setEnabled(false);
            word.setEnabled(false);
            direction.setEnabled(false);
            status.setForeground(muted);
            status.setText("Поиск перевода…");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { return translate(inputWord, selected); }
                @Override protected void done() {
                    try {
                        result.setText(get());
                        result.setCaretPosition(0);
                        copy.setEnabled(true);
                        status.setText(" ");
                    } catch (Exception e) {
                        status.setForeground(error);
                        status.setText(errorMessage(e));
                    } finally {
                        translate.setEnabled(true);
                        word.setEnabled(true);
                        direction.setEnabled(true);
                        word.requestFocusInWindow();
                        word.selectAll();
                    }
                }
            }.execute();
        });
        word.addActionListener(event -> translate.doClick());

        // --- Вкладка презентации ---
        JButton loadPresentationBtn = new JButton("Загрузить презентацию");
        JTextArea fileStatus = note("Презентация будет скачана и открыта прямо в этом окне.", 2);

        JLabel slideLabel = new JLabel();
        slideLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane slideScroll = new JScrollPane(slideLabel);

        JButton prevBtn = new JButton("<< Назад");
        JButton nextBtn = new JButton("Вперед >>");
        JLabel counterLabel = new JLabel("Слайд: 0 / 0");

        prevBtn.setEnabled(false);
        nextBtn.setEnabled(false);

        JPanel controlsPanel = new JPanel(new FlowLayout());
        controlsPanel.add(prevBtn);
        controlsPanel.add(counterLabel);
        controlsPanel.add(nextBtn);

        JPanel presentation = new JPanel(new BorderLayout(0, 10));
        presentation.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.add(loadPresentationBtn, BorderLayout.WEST);
        topPanel.add(fileStatus, BorderLayout.CENTER);

        presentation.add(topPanel, BorderLayout.NORTH);
        presentation.add(slideScroll, BorderLayout.CENTER);
        presentation.add(controlsPanel, BorderLayout.SOUTH);

        final List<BufferedImage>[] presentationSlides = new List[]{null};
        final int[] currentSlide = {0};

        prevBtn.addActionListener(e -> {
            if (currentSlide[0] > 0) {
                currentSlide[0]--;
                slideLabel.setIcon(new ImageIcon(presentationSlides[0].get(currentSlide[0])));
                counterLabel.setText("Слайд: " + (currentSlide[0] + 1) + " / " + presentationSlides[0].size());
                nextBtn.setEnabled(true);
                if (currentSlide[0] == 0) prevBtn.setEnabled(false);
            }
        });

        nextBtn.addActionListener(e -> {
            if (currentSlide[0] < presentationSlides[0].size() - 1) {
                currentSlide[0]++;
                slideLabel.setIcon(new ImageIcon(presentationSlides[0].get(currentSlide[0])));
                counterLabel.setText("Слайд: " + (currentSlide[0] + 1) + " / " + presentationSlides[0].size());
                prevBtn.setEnabled(true);
                if (currentSlide[0] == presentationSlides[0].size() - 1) nextBtn.setEnabled(false);
            }
        });

        loadPresentationBtn.addActionListener(event -> {
            loadPresentationBtn.setEnabled(false);
            fileStatus.setForeground(muted);
            fileStatus.setText("Скачивание и рендеринг (может занять несколько секунд)…");

            new SwingWorker<List<BufferedImage>, Void>() {
                @Override protected List<BufferedImage> doInBackground() throws Exception {
                    return PresentationClient.downloadAndRender();
                }

                @Override protected void done() {
                    try {
                        presentationSlides[0] = get();
                        currentSlide[0] = 0;
                        slideLabel.setIcon(new ImageIcon(presentationSlides[0].get(0)));
                        counterLabel.setText("Слайд: 1 / " + presentationSlides[0].size());
                        fileStatus.setText("Презентация загружена!");

                        prevBtn.setEnabled(false);
                        if (presentationSlides[0].size() > 1) {
                            nextBtn.setEnabled(true);
                        }
                    } catch (Exception e) {
                        fileStatus.setForeground(error);
                        fileStatus.setText(errorMessage(e));
                    } finally {
                        fileStatus.setCaretPosition(0);
                        loadPresentationBtn.setEnabled(true);
                    }
                }
            }.execute();
        });

        // --- Собираем все вкладки ---
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Словарь", dictionary);
        tabs.addTab("Презентация", presentation);
        tabs.addTab("Котировки", createQuotesContent());

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        content.add(tabs, BorderLayout.CENTER);
        content.putClientProperty("translateButton", translate);
        content.putClientProperty("wordField", word);

        tabs.addChangeListener(event -> {
            JRootPane root = SwingUtilities.getRootPane(tabs);
            if (root != null) {
                JButton primary = tabs.getSelectedIndex() == 0 ? translate : loadPresentationBtn;
                root.setDefaultButton(primary);
                setPrimaryText(primary);
            }
        });

        return content;
    }

    // --- Метод для получения котировок с Yahoo Finance ---
    public static String fetchPrice(String ticker, String name) throws Exception {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker;

        var config = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(5000).build();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new java.io.IOException("Ошибка подключения к бирже (HTTP " + response.getStatusLine().getStatusCode() + ")");
                }
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"regularMarketPrice\":([\\d.]+)").matcher(json);
                if (matcher.find()) {
                    return "Текущая цена (" + name + "): " + matcher.group(1) + " USD";
                }
                throw new java.io.IOException("Не удалось распознать цену в ответе сервера.");
            }
        }
    }

    // --- Создание вкладки "Котировки" ---
    static JPanel createQuotesContent() {
        Color muted = UIManager.getColor("Label.foreground");
        Color error = new Color(166, 42, 42);

        JButton btnOil = new JButton("Узнать курс Нефти (WTI)");
        JButton btnPlatinum = new JButton("Узнать курс Платины");

        JLabel resultLabel = new JLabel("Выберите ресурс для получения цены", SwingConstants.CENTER);
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 16f));

        JTextArea status = note("Данные берутся в реальном времени с Yahoo Finance.", 2);
        status.setOpaque(false);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonsPanel.add(btnOil);
        buttonsPanel.add(btnPlatinum);

        JPanel quotes = new JPanel(new BorderLayout(0, 20));
        quotes.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
        quotes.add(buttonsPanel, BorderLayout.NORTH);
        quotes.add(resultLabel, BorderLayout.CENTER);
        quotes.add(status, BorderLayout.SOUTH);

        java.awt.event.ActionListener fetchAction = event -> {
            boolean isOil = event.getSource() == btnOil;
            String ticker = isOil ? "CL=F" : "PL=F";
            String name = isOil ? "Нефть" : "Платина";

            btnOil.setEnabled(false);
            btnPlatinum.setEnabled(false);
            resultLabel.setForeground(muted);
            resultLabel.setText("Загрузка данных...");

            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception {
                    return fetchPrice(ticker, name);
                }
                @Override protected void done() {
                    try {
                        resultLabel.setForeground(new Color(34, 139, 34)); // Зеленый цвет
                        resultLabel.setText(get());
                    } catch (Exception e) {
                        resultLabel.setForeground(error);
                        resultLabel.setText(errorMessage(e));
                    } finally {
                        btnOil.setEnabled(true);
                        btnPlatinum.setEnabled(true);
                    }
                }
            }.execute();
        };

        btnOil.addActionListener(fetchAction);
        btnPlatinum.addActionListener(fetchAction);

        return quotes;
    }

    private static void setPrimaryText(JButton button) {
        if ("Aqua".equals(UIManager.getLookAndFeel().getID())) {
            button.setForeground(Color.WHITE);
        }
    }

    private static JTextArea note(String text, int rows) {
        JTextArea area = new JTextArea(text, rows, 32);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(UIManager.getFont("Label.font"));
        area.setForeground(UIManager.getColor("Label.foreground"));
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        area.setPreferredSize(new Dimension(360, rows * lineHeight + 4));
        return area;
    }

    private static String errorMessage(Exception error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        if (cause instanceof java.net.ConnectException || cause instanceof java.net.UnknownHostException) {
            return "Не удалось подключиться к серверу. Проверьте, что GlassFish запущен и приложение развёрнуто.";
        }
        if (cause instanceof java.net.SocketTimeoutException) {
            return "Сервер долго не отвечает. Попробуйте ещё раз.";
        }
        return cause.getMessage() == null ? "Не удалось выполнить запрос. Попробуйте ещё раз." : cause.getMessage();
    }
}