package by.bsuir.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PresentationClient {
    private static final String MIME = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private PresentationClient() { }

    public static Path download() throws IOException {
        URI url = URI.create(System.getProperty("presentation.url",
                URI.create(ApacheClient.URL).resolve("PresentationServlet").toString()));
        RequestConfig config = RequestConfig.custom().setConnectTimeout(5000)
                .setSocketTimeout(30000).setConnectionRequestTimeout(5000).build();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();
             CloseableHttpResponse response = client.execute(new HttpGet(url))) {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                if (status == 404) {
                    throw new IOException("Презентация не найдена. Добавьте presentation.pptx на сервер "
                            + "и выполните package и Redeploy. Адрес: " + url);
                }
                throw new IOException("Не удалось скачать презентацию: HTTP " + status);
            }
            if (response.getEntity() == null
                    || !MIME.equalsIgnoreCase(ContentType.getOrDefault(response.getEntity()).getMimeType())) {
                throw new IOException("Сервер вернул ответ, который не является презентацией PowerPoint.");
            }
            Path directory = Files.createTempDirectory("lab3-presentation-");
            Path file = directory.resolve("presentation.pptx");
            try (var stream = response.getEntity().getContent()) {
                Files.copy(stream, file);
                if (Files.size(file) == 0) throw new IOException("Сервер вернул пустую презентацию.");
                return file;
            } catch (IOException e) {
                Files.deleteIfExists(file);
                Files.deleteIfExists(directory);
                throw e;
            }
        }
    }

    public static Path downloadAndOpen() throws IOException {
        Path file = download();
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("Автоматическое открытие файлов не поддерживается.");
            }
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            throw new IOException("Не удалось открыть презентацию. Файл сохранён: " + file
                    + ". Проверьте приложение по умолчанию для .pptx.", e);
        }
        // Keep the file after the client exits: PowerPoint can continue using it.
        return file;
    }
}
