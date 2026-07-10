package com.mapleunion.vo;

/**
 * 캐릭터 한 명의 정보를 담는 객체 (Value Object)
 * DB 테이블의 한 행(row)과 1:1로 매칭됩니다.
 */
public class CharacterVO {

    private int charId;        // 자동 증가 PK (DB가 알아서 채워줌)
    private String charName;   // 캐릭터명
    private String job;        // 전직
    private int level;         // 레벨

    public CharacterVO() {
    }

    public CharacterVO(String charName, String job, int level) {
        this.charName = charName;
        this.job = job;
        this.level = level;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public String getCharName() {
        return charName;
    }

    public void setCharName(String charName) {
        this.charName = charName;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
