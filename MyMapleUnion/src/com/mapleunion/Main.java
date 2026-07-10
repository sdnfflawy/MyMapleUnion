package com.mapleunion;

import javax.swing.SwingUtilities;

import com.mapleunion.util.DBUtil;
import com.mapleunion.view.MainFrame;

public class Main {

    public static void main(String[] args) {

        // 1) 프로그램 시작 시 DB(테이블)가 없으면 생성
        DBUtil.initDB();

        // 2) Swing 화면은 항상 이벤트 디스패치 스레드(EDT)에서 띄우는 것이 규칙입니다.
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
