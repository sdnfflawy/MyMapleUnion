package com.mapleunion.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mapleunion.util.DBUtil;
import com.mapleunion.vo.CharacterVO;

/**
 * character_info 테이블에 대한 입력/조회/통계를 담당합니다.
 * (Spring 없이 순수 JDBC로 작성했기 때문에 Service 계층 없이
 *  DAO가 바로 화면 클래스에서 호출됩니다)
 */
public class CharacterDAO {

    /** 캐릭터 한 명을 테이블에 저장합니다. */
    public void insert(CharacterVO vo) {
        String sql = "INSERT INTO character_info(char_name, job, level) VALUES(?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vo.getCharName());
            pstmt.setString(2, vo.getJob());
            pstmt.setInt(3, vo.getLevel());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 현재까지 저장된 캐릭터 수를 반환합니다. (42개 제한 체크용) */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM character_info";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 전체 캐릭터를 레벨 높은 순으로 정렬해서 반환합니다. */
    public List<CharacterVO> selectAllOrderByLevelDesc() {
        List<CharacterVO> list = new ArrayList<>();
        String sql = "SELECT char_id, char_name, job, level "
                + "FROM character_info ORDER BY level DESC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                CharacterVO vo = new CharacterVO();
                vo.setCharId(rs.getInt("char_id"));
                vo.setCharName(rs.getString("char_name"));
                vo.setJob(rs.getString("job"));
                vo.setLevel(rs.getInt("level"));
                list.add(vo);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 특정 캐릭터(char_id)의 캐릭터명을 수정합니다. */
    public void updateCharName(int charId, String newName) {
        String sql = "UPDATE character_info SET char_name = ? WHERE char_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newName);
            pstmt.setInt(2, charId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 특정 캐릭터(char_id)의 전직을 수정합니다. */
    public void updateJob(int charId, String newJob) {
        String sql = "UPDATE character_info SET job = ? WHERE char_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newJob);
            pstmt.setInt(2, charId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 특정 캐릭터(char_id)의 레벨을 수정합니다. */
    public void updateLevel(int charId, int newLevel) {
        String sql = "UPDATE character_info SET level = ? WHERE char_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newLevel);
            pstmt.setInt(2, charId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 전체 레벨 합계를 DB에서 바로 계산해서 반환합니다. */
    public int sumLevel() {
        String sql = "SELECT COALESCE(SUM(level), 0) FROM character_info";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 등록된 캐릭터 전체를 삭제하고, char_id 자동증가 번호도 1부터 다시 시작하도록 초기화합니다. */
    public void deleteAll() {
        String deleteSql = "DELETE FROM character_info";
        String resetSeqSql = "DELETE FROM sqlite_sequence WHERE name = 'character_info'";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(deleteSql);
            stmt.executeUpdate(resetSeqSql); // AUTOINCREMENT 카운터 초기화 (없어도 에러 아님)

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}