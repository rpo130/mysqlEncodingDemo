package pr.rpo;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.sql.*;

@Testcontainers
public class Test47 {

    public static MySQLContainer<?> mysqlCustomConfig =
            new MySQLContainer<>(DockerImageName.parse("mysql:5.6"));

    static Connection conn = null;
    static Statement stmt = null;
    static URLClassLoader urlClassLoader;

    @BeforeAll
    public static void init() throws ClassNotFoundException, SQLException, URISyntaxException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
       mysqlCustomConfig.start();
    }

    @AfterAll
    public static void after() throws SQLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        mysqlCustomConfig.close();
    }

    @BeforeEach
    public void initEach() throws SQLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, URISyntaxException, MalformedURLException, InstantiationException {
        Class.forName("com.mysql.jdbc.Driver");
        String mysqlURL = mysqlCustomConfig.getJdbcUrl() + "?characterEncoding=UTF-8";
        conn = DriverManager.getConnection(mysqlURL, mysqlCustomConfig.getUsername(), mysqlCustomConfig.getPassword());
        stmt = conn.createStatement();

        String createTable =
                """
                    CREATE TABLE `test` (
                        `str` VARCHAR(50) NULL DEFAULT NULL COLLATE 'utf8mb4_general_ci'
                    )
                    COLLATE='latin1_swedish_ci'
                    ENGINE=InnoDB
                    ;
                """;

        stmt.execute(createTable);
    }

    @AfterEach
    public void afterEach() throws SQLException {

        String dropTable =
                """
                    drop TABLE `test`;
                """;

        stmt.execute(dropTable);
        stmt.close();
        conn.close();
    }

    public void showVariable() throws SQLException {
        String sql = "show session variables like 'char%'";
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            System.out.print(rs.getString("Variable_name"));
            System.out.print(" ");
            System.out.println(rs.getString("Value"));
        }

        rs.close();
    }

    private String getEmojiFromDb() throws SQLException {
        String sql1 = "select str from test;";
        ResultSet rs = stmt.executeQuery(sql1);
        String str = null;
        while (rs.next()) {
            str = rs.getString(1);
            System.out.println(str);
            break;
        }
        return str;
    }


    @Test
    public void testInsertEmoji() throws SQLException {
        showVariable();
        String sql = "insert into test(str) values ('😊');";
        stmt.executeUpdate(sql);
        Assertions.assertEquals("😊", getEmojiFromDb());
    }
}
