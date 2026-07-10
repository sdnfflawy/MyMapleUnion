package com.mapleunion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite DB 연결과 초기화(테이블 생성)를 담당합니다.
 *
 * 포인트:
 * - SQLite는 "서버"가 아니라 "파일"입니다.
 * - jdbc:sqlite:mymapleunion.db 라고 쓰면, 프로그램이 실행되는 폴더에
 *   mymapleunion.db 라는 파일이 생성되고 그 파일이 곧 DB입니다.
 * - 이 파일 하나만 있으면 데이터가 유지되므로, exe와 함께 이 파일을
 *   옮기면 다른 컴퓨터에서도 데이터가 이어집니다. (없으면 새로 생성)
 */
public class DBUtil {

    private static final String DB_URL = "jdbc:sqlite:mymapleunion.db";

    // 커넥션을 필요할 때마다 새로 만들어서 반환합니다.
    // (SQLite는 가벼운 파일 기반 DB라 매번 새로 열고 닫아도 부담이 없습니다)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 프로그램 최초 실행 시 호출됩니다.
     * 테이블이 없으면 새로 만들고, 이미 있으면 아무 일도 하지 않습니다.
     * (IF NOT EXISTS 덕분에 재실행해도 안전합니다)
     */
    public static void initDB() {
        String sql = "CREATE TABLE IF NOT EXISTS character_info ("
                + "char_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "char_name TEXT NOT NULL,"
                + "job TEXT NOT NULL,"
                + "level INTEGER NOT NULL"
                + ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
