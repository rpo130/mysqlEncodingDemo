package pr.rpo;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URLClassLoader;
import java.sql.*;

@Testcontainers
public class Test45 {

    public static MySQLContainer<?> mysqlCustomConfig =
            new MySQLContainer<>(DockerImageName.parse("mysql:5.6"));

    static Connection conn = null;
    static Statement stmt = null;
    static URLClassLoader urlClassLoader;

    @BeforeAll
    public static void init() {
        mysqlCustomConfig.start();
    }

    @AfterAll
    public static void after() {
        mysqlCustomConfig.close();
    }

    @BeforeEach
    public void initEach() throws SQLException, ClassNotFoundException {
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
        Assertions.assertThrows(SQLException.class, () -> stmt.executeUpdate(sql));
    }

    @Test
    public void testInsertEmojiWithCharacterIntroducer() throws SQLException {
        showVariable();
        String sql = "insert into test(str) values (_utf8mb4 '😊');";
        stmt.executeUpdate(sql);
        Assertions.assertEquals("😊", getEmojiFromDb());
    }

    @Test
    public void testInsertEmojiWithCharacterIntroducerAndResultCharSetChangeTolatin1() throws SQLException {
        setResultCharSet("latin1");
        showVariable();
        String sql = "insert into test(str) values (_utf8mb4 '😊');";
        stmt.executeUpdate(sql);

        String sql1 = "select str from test;";
        ResultSet rs = stmt.executeQuery(sql1);
        String str = null;
        while (rs.next()) {
            str = rs.getString(1);
            System.out.println(str);
            break;
        }
        Assertions.assertEquals("?", str);
    }

    @Test
    public void testInsertEmojiWithSetNamesManual() throws SQLException {
        String ssql = "set names utf8mb4";
        stmt.executeUpdate(ssql);

        showVariable();
        String sql = "insert into test(str) values ('😊');";
        stmt.executeUpdate(sql);
        Assertions.assertEquals("😊", getEmojiFromDb());
    }

    @Test
    public void testInsertEmojiWithOnlyClientCharSetChangeToUtf8mb4() throws SQLException {
        setClientCharSet("utf8mb4");

        showVariable();
        String sql = "insert into test(str) values ('😊');";
        stmt.executeUpdate(sql);

        String str = getEmojiFromDb();

        Assertions.assertEquals("?", str);
    }

    @Test
    public void testInsertEmojiWithOnlyConnectionCharSetChangeToUtf8mb4() throws SQLException {
        setConnectionCharSet("utf8mb4");

        showVariable();
        String sql = "insert into test(str) values ('😊');";
        stmt.executeUpdate(sql);

        String str = getEmojiFromDb();
        Assertions.assertEquals("????", str);
    }

    @Test
    public void testInsertEmojiWithBothConnectionCharSetAndClientCharSetChangeToUtf8mb4() throws SQLException {
        setClientCharSet("utf8mb4");
        setConnectionCharSet("utf8mb4");

        showVariable();
        String sql = "insert into test(str) values ('😊');";
        stmt.executeUpdate(sql);

        String str = getEmojiFromDb();
        Assertions.assertEquals("😊", str);
    }

    private void setClientCharSet(String charSet) throws SQLException {
        String ssql = "set session character_set_client=" + charSet;
        stmt.executeUpdate(ssql);
    }

    private void setConnectionCharSet(String charSet) throws SQLException {
        String ssql = "set session character_set_connection=" + charSet;
        stmt.executeUpdate(ssql);
    }

    private void setResultCharSet(String charSet) throws SQLException {
        String ssql = "set session character_set_results=" + charSet;
        stmt.executeUpdate(ssql);
    }
}
