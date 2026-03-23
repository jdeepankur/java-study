import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.testapp.model.Database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseTest {

    private Connection mockConnection;
    private Statement mockStatement;

    @BeforeEach
    void setUp() throws SQLException, IllegalAccessException, NoSuchFieldException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        Field connectionField = Database.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(null, mockConnection);
    }

    // --- Field privacy tests ---

    @Test
    void connectionFieldIsPrivate() throws NoSuchFieldException {
        Field field = Database.class.getDeclaredField("connection");
        assertTrue(Modifier.isPrivate(field.getModifiers()));
    }

    @Test
    void urlFieldIsPrivateStaticFinal() throws NoSuchFieldException {
        Field field = Database.class.getDeclaredField("url");
        int mods = field.getModifiers();
        assertTrue(Modifier.isPrivate(mods));
        assertTrue(Modifier.isStatic(mods));
        assertTrue(Modifier.isFinal(mods));
    }

    @Test
    void portFieldIsPrivateStaticFinal() throws NoSuchFieldException {
        Field field = Database.class.getDeclaredField("port");
        int mods = field.getModifiers();
        assertTrue(Modifier.isPrivate(mods));
        assertTrue(Modifier.isStatic(mods));
        assertTrue(Modifier.isFinal(mods));
    }

    // --- createTable tests ---

    @Test
    void createTableExecutesCorrectSQL() throws SQLException {
        String[] columns = {"name", "email"};
        Database.createTable("users", columns);

        verify(mockStatement).execute(
            "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), email VARCHAR(255))"
        );
    }

    @Test
    void createTableWithSingleColumn() throws SQLException {
        String[] columns = {"name"};
        Database.createTable("users", columns);

        verify(mockStatement).execute(
            "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255))"
        );
    }

    @Test
    void createTableThrowsWhenConnectionFails() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Connection lost"));

        assertThrows(SQLException.class, () -> Database.createTable("users", new String[]{"name"}));
    }

    // --- insertValue tests ---

    @Test
    void insertValueExecutesCorrectSQL() throws SQLException {
        String[] columns = {"name"};
        String[] values = {"'Alice'"};

        Database.insertValue("users", columns, values);

        verify(mockStatement).execute("INSERT INTO users (name) VALUES ('Alice')");
    }

    @Test
    void insertValueWithMultipleColumns() throws SQLException {
        String[] columns = {"name", "email"};
        String[] values = {"'Bob'", "'bob@test.com'"};

        Database.insertValue("users", columns, values);

        verify(mockStatement).execute(
            "INSERT INTO users (name, email) VALUES ('Bob', 'bob@test.com')"
        );
    }

    @Test
    void insertValueThrowsWhenConnectionFails() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Connection lost"));

        assertThrows(SQLException.class, () ->
            Database.insertValue("users", new String[]{"name"}, new String[]{"'Alice'"})
        );
    }
}
