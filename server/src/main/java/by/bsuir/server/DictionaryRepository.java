package by.bsuir.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.h2.tools.RunScript;
import org.h2.jdbcx.JdbcDataSource;

public final class DictionaryRepository {
    private final JdbcDataSource dataSource = new JdbcDataSource();

    public DictionaryRepository(String url) throws SQLException, IOException {
        dataSource.setURL(url);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        try (Connection connection = dataSource.getConnection();
             InputStream stream = Objects.requireNonNull(getClass().getResourceAsStream("/dictionary.sql"));
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }

    public List<String> translate(String word, String direction) throws SQLException {
        String source = direction.equals("ru-en") ? "russian" : "english";
        String target = direction.equals("ru-en") ? "english" : "russian";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT " + target + " FROM dictionary WHERE " + source + " = ? ORDER BY " + target)) {
            query.setString(1, word.strip().toLowerCase(Locale.ROOT));
            try (ResultSet rows = query.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rows.next()) result.add(rows.getString(1));
                return result;
            }
        }
    }
}
