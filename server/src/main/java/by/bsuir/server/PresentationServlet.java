package by.bsuir.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class PresentationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (InputStream file = getClass().getResourceAsStream("/presentations/presentation.pptx")) {
            if (file == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().print("Презентация ещё не добавлена на сервер. "
                        + "Поместите presentation.pptx в server/src/main/resources/presentations, "
                        + "затем выполните package и Redeploy.");
                return;
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            response.setHeader("Content-Disposition", "attachment; filename=\"presentation.pptx\"");
            // Give Office both the file size (including for HEAD) and the binary content.
            byte[] content = file.readAllBytes();
            response.setContentLengthLong(content.length);
            response.getOutputStream().write(content);
        }
    }
}
