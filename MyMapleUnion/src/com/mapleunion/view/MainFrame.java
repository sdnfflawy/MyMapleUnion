package com.mapleunion.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.mapleunion.dao.CharacterDAO;
import com.mapleunion.util.DBUtil;
import com.mapleunion.vo.CharacterVO;

/**
 * 프로그램의 메인 화면입니다.
 *
 * 동작 방식:
 * 1) 캐릭터 수가 42개 미만이면 -> 입력 폼이 활성화되어 있고,
 *    등록할 때마다 테이블에 (등록한 순서대로) 한 줄씩 추가됩니다.
 * 2) 42개를 다 채우면 -> 입력 폼이 잠기고, 테이블이 레벨 내림차순으로
 *    다시 정렬되어 표시되며, 레벨 총합이 화면에 나타납니다.
 */
public class MainFrame extends JFrame {

    private static final int MAX_CHARACTER_COUNT = 42;

    private final CharacterDAO dao = new CharacterDAO();

    // 입력 컴포넌트
    private JTextField nameField;
    private JTextField jobField;
    private JTextField levelField;
    private JButton registerButton;
    private JButton resetButton; // 등록된 데이터를 전부 초기화하는 버튼

    // 상태 표시
    private JLabel statusLabel;   // "현재 10 / 42 명 등록됨" 같은 문구
    private JLabel sumLabel;      // 완성 후 레벨 합계

    // 결과 테이블 (좌: 1~21번째, 우: 22~42번째)
    private static final int ROWS_PER_TABLE = 21;
    private JTable leftTable;
    private JTable rightTable;
    private DefaultTableModel leftTableModel;
    private DefaultTableModel rightTableModel;

    // 42명이 다 채워진 뒤(완성 모드)에만 true가 되며, 이때만 셀 편집이 허용됩니다.
    private boolean editingEnabled = false;

    // 현재 좌/우 테이블에 표시 중인 실제 데이터(패딩 빈 행 제외).
    // 테이블의 row 번호 -> char_id 를 알아내기 위한 용도입니다.
    private List<CharacterVO> leftList = new java.util.ArrayList<>();
    private List<CharacterVO> rightList = new java.util.ArrayList<>();

    // revertCell()로 셀 값을 되돌릴 때, 그로 인한 이벤트를 다시 "수정"으로 처리하지 않기 위한 플래그
    private boolean suppressTableEvents = false;

    public MainFrame() {
        setTitle("MyMapleUnion - 캐릭터 관리");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 프로그램 아이콘 설정 (같은 패키지의 icon.png 사용)
        java.net.URL iconURL = getClass().getResource("icon.png");
        if (iconURL != null) {
            setIconImage(new javax.swing.ImageIcon(iconURL).getImage());
        } else {
            System.err.println("아이콘 파일을 찾을 수 없습니다: icon.png");
        }

        initComponents();

        // 표를 먼저 21행(실데이터 + 빈 행 패딩)으로 채운 뒤에 pack()을 호출해야
        // 창 크기가 실제 표 크기에 맞춰 정확히 계산됩니다. (순서 중요)
        refreshTableFromDB(); // 프로그램 시작 시 기존 데이터가 있으면 불러오기

        pack();                      // 모든 컴포넌트의 실제 크기에 맞춰 창 크기를 자동 계산
        setResizable(false);         // 창 크기 고정 (최대화/드래그 리사이즈 불가)
        setLocationRelativeTo(null); // 화면 중앙에 띄우기
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // ---------- 상단: 입력 폼 ----------
        // GridBagLayout으로 라벨을 입력칸에 붙이고, 입력칸 폭을 "전직" 최대 길이에 맞춰
        // 좁게 잡은 뒤 오른쪽 남는 공간은 배너 이미지가 채웁니다.
        JPanel inputForm = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        inputForm.add(new JLabel("캐릭터명"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(16); // "아크메이지(얼음,번개)" 기준 폭
        inputForm.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputForm.add(new JLabel("전직"), gbc);
        gbc.gridx = 1;
        jobField = new JTextField(16);
        inputForm.add(jobField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputForm.add(new JLabel("레벨"), gbc);
        gbc.gridx = 1;
        levelField = new JTextField(16);
        inputForm.add(levelField, gbc);

        registerButton = new JButton("등록");
        registerButton.addActionListener(new RegisterButtonListener());
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        inputForm.add(registerButton, gbc);

        // Enter 키를 누르면 (캐릭터명/전직/레벨 입력칸 중 어디에 포커스가 있든)
        // 마우스로 등록 버튼을 클릭한 것과 동일하게 동작합니다.
        getRootPane().setDefaultButton(registerButton);

        // 배너 이미지 (오른쪽 여백을 정확히 채움)
        java.net.URL bannerURL = getClass().getResource("banner.jpg");
        if (bannerURL != null) {
            JLabel bannerLabel = new JLabel(new javax.swing.ImageIcon(bannerURL));
            gbc.gridx = 2; gbc.gridy = 0;
            gbc.gridheight = 4;
            gbc.weightx = 1.0;
            gbc.fill = java.awt.GridBagConstraints.BOTH;
            inputForm.add(bannerLabel, gbc);
        } else {
            System.err.println("배너 이미지를 찾을 수 없습니다: banner.jpg");
        }

        inputForm.setBorder(javax.swing.BorderFactory.createTitledBorder("캐릭터 정보 입력"));
        add(inputForm, BorderLayout.NORTH);

        // ---------- 중앙: 결과 테이블 (좌 1~21 / 우 22~42로 2분할) ----------
        String[] columnNames = { "캐릭터명", "전직", "레벨" };

        final int rowHeight = 22;

        leftTableModel = createTableModel(columnNames);
        leftTable = new JTable(leftTableModel);
        leftTable.setRowHeight(rowHeight);
        leftTable.getTableHeader().setReorderingAllowed(false); // 헤더 드래그로 열 순서 바꾸는 것 금지
        leftTable.getColumnModel().getColumn(2).setCellEditor(new LevelSpinnerEditor());

        rightTableModel = createTableModel(columnNames);
        rightTable = new JTable(rightTableModel);
        rightTable.setRowHeight(rowHeight);
        rightTable.getTableHeader().setReorderingAllowed(false); // 헤더 드래그로 열 순서 바꾸는 것 금지
        rightTable.getColumnModel().getColumn(2).setCellEditor(new LevelSpinnerEditor());

        // 캐릭터명/전직/레벨 셀 내용을 모두 가운데 정렬 (가독성 향상)
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        for (int col = 0; col < columnNames.length; col++) {
            leftTable.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
            rightTable.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
        }

        // 완성 모드에서 셀 편집이 끝나면(더블클릭 -> 타이핑 -> 포커스 아웃/엔터) 실제 DB에도 반영합니다.
        // 주의: leftList/rightList는 이후 fillTable()에서 항상 clear()+addAll()로만 갱신되고
        //       절대 새 리스트로 재할당되지 않으므로, 여기서 넘기는 참조가 계속 유효합니다.
        leftTableModel.addTableModelListener(new CellEditListener(leftTableModel, leftList));
        rightTableModel.addTableModelListener(new CellEditListener(rightTableModel, rightList));

        // 표는 항상 정확히 21행으로 고정되어(fillSingleTable에서 빈 행으로 패딩) 스크롤이
        // 전혀 필요 없으므로, JScrollPane 없이 헤더 + 테이블을 직접 패널에 붙입니다.
        // -> 마우스 휠에 반응해 표가 미세하게 움직이는 현상이 원천적으로 사라지고,
        //    pack()이 실제 21행 크기 그대로 정확히 계산해서 끝 행이 잘리지도 않습니다.
        JPanel leftTablePanel = new JPanel(new BorderLayout());
        leftTablePanel.add(leftTable.getTableHeader(), BorderLayout.NORTH);
        leftTablePanel.add(leftTable, BorderLayout.CENTER);

        JPanel rightTablePanel = new JPanel(new BorderLayout());
        rightTablePanel.add(rightTable.getTableHeader(), BorderLayout.NORTH);
        rightTablePanel.add(rightTable, BorderLayout.CENTER);

        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        tablesPanel.add(leftTablePanel);
        tablesPanel.add(rightTablePanel);

        add(tablesPanel, BorderLayout.CENTER);

        // ---------- 하단: 상태 / 합계 / 초기화 ----------
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel();
        sumLabel = new JLabel();
        bottomPanel.add(statusLabel);
        bottomPanel.add(sumLabel);

        // 초기화 버튼: 1명 이상 등록된 순간부터(완성 후 포함) 보이고, 0명일 땐 숨겨집니다.
        resetButton = new JButton("데이터 초기화");
        resetButton.setForeground(java.awt.Color.RED);
        resetButton.addActionListener(new ResetButtonListener());
        resetButton.setVisible(false); // 시작 시 0명이면 안 보이도록 기본값은 숨김
        bottomPanel.add(javax.swing.Box.createHorizontalStrut(20)); // 상태 텍스트와 살짝 간격
        bottomPanel.add(resetButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * "등록" 버튼 클릭 시 실행되는 로직입니다.
     */
    private class RegisterButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            // 1) 이미 42명이 다 찼는지 먼저 확인
            int currentCount = dao.countAll();
            if (currentCount >= MAX_CHARACTER_COUNT) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "이미 42명이 모두 등록되어 더 이상 입력할 수 없습니다.",
                        "등록 불가", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2) 입력값 꺼내기
            String name = nameField.getText().trim();
            String job = jobField.getText().trim();
            String levelText = levelField.getText().trim();

            // 3) 유효성 검사 (빈 값, 레벨이 숫자인지)
            if (name.isEmpty() || job.isEmpty() || levelText.isEmpty()) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "캐릭터명, 전직, 레벨을 모두 입력해 주세요.",
                        "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int level;
            try {
                level = Integer.parseInt(levelText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "레벨은 숫자로 입력해 주세요.",
                        "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 4) DB에 저장
            CharacterVO vo = new CharacterVO(name, job, level);
            dao.insert(vo);

            // 5) 입력창 비우기
            nameField.setText("");
            jobField.setText("");
            levelField.setText("");
            nameField.requestFocus();

            // 6) 새로 등록했으니 카운트를 다시 확인
            int newCount = dao.countAll();

            if (newCount >= MAX_CHARACTER_COUNT) {
                // 42명이 다 채워진 순간 -> 완성 모드로 전환
                completeRegistration();
            } else {
                // 아직 미완성 -> 등록 순서 그대로 보여주고 상태만 갱신
                refreshTableFromDB();
            }
        }
    }

    /**
     * "데이터 초기화" 버튼 클릭 시 실행되는 로직입니다.
     * 진행 중이든(1~41명) 완성 상태(42명)든 상관없이, 확인 후 전체 삭제하고
     * 프로그램을 "0명 등록" 상태(입력 가능 모드)로 되돌립니다.
     */
    private class ResetButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                    "등록된 모든 캐릭터 정보가 영구적으로 삭제됩니다.\n정말 초기화하시겠습니까?",
                    "데이터 초기화 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return; // 취소 시 아무 것도 하지 않음
            }

            // 1) DB 전체 삭제
            dao.deleteAll();

            // 2) 입력 폼 다시 활성화 + 비우기
            nameField.setEnabled(true);
            jobField.setEnabled(true);
            levelField.setEnabled(true);
            registerButton.setEnabled(true);
            nameField.setText("");
            jobField.setText("");
            levelField.setText("");
            nameField.requestFocus();

            // 3) 편집(완성) 모드 해제 + 화면상의 매핑 리스트 비우기
            editingEnabled = false;
            leftList.clear();
            rightList.clear();
            fillSingleTable(leftTableModel, leftList);
            fillSingleTable(rightTableModel, rightList);

            // 4) 상태 표시 초기화
            statusLabel.setText("현재 0 / " + MAX_CHARACTER_COUNT + " 명 등록됨");
            sumLabel.setText("");

            // 5) 0명이 되었으니 초기화 버튼 다시 숨김
            resetButton.setVisible(false);
        }
    }

    /**
     * 42명이 모두 채워졌을 때 호출됩니다.
     * - 입력 폼을 잠그고
     * - 테이블을 레벨 내림차순으로 다시 그리고
     * - 레벨 합계를 계산해서 보여줍니다.
     */
    private void completeRegistration() {
        nameField.setEnabled(false);
        jobField.setEnabled(false);
        levelField.setEnabled(false);
        registerButton.setEnabled(false);

        List<CharacterVO> sortedList = dao.selectAllOrderByLevelDesc();
        fillTable(sortedList);

        // 42명이 다 채워졌으므로 이제부터는 캐릭터명/전직 셀을 더블클릭해서 수정할 수 있습니다.
        editingEnabled = true;
        resetButton.setVisible(true); // 42명(1명 이상)이므로 계속 보임

        int total = dao.sumLevel();
        statusLabel.setText("총 42명 등록 완료 (레벨 내림차순 정렬) - 캐릭터명/전직은 더블클릭으로 수정 가능");
        sumLabel.setText("   |   레벨 총합: " + total);
    }

    /**
     * 아직 등록이 진행 중일 때, 화면을 최신 상태로 갱신합니다.
     * (등록 순서 그대로 보여주고, 진행 상황 카운트를 표시)
     */
    private void refreshTableFromDB() {
        int count = dao.countAll();

        if (count >= MAX_CHARACTER_COUNT) {
            // 프로그램을 종료했다가 다시 켰는데 이미 42명이 채워져 있는 경우
            completeRegistration();
            return;
        }

        // 1명 이상이면 초기화 버튼을 보여주고, 0명이면 숨깁니다.
        resetButton.setVisible(count > 0);

        // 등록 순서(char_id 오름차순)대로 보여줌
        fillTable(selectAllInInsertOrder());
        statusLabel.setText("현재 " + count + " / " + MAX_CHARACTER_COUNT + " 명 등록됨");
        sumLabel.setText("");
    }

    /**
     * 테이블 모델을 생성하는 헬퍼 메서드.
     * - 42명 입력이 끝나기 전(editingEnabled == false)에는 항상 조회 전용입니다.
     * - 완성 모드(editingEnabled == true)에서는 "캐릭터명"(0), "전직"(1), "레벨"(2)
     *   컬럼 모두 더블클릭으로 수정할 수 있습니다.
     */
    private DefaultTableModel createTableModel(String[] columnNames) {
        return new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return editingEnabled;
            }
        };
    }

    /**
     * 완성 모드에서 셀 편집(더블클릭 -> 타이핑 -> 포커스 아웃/Enter)이 끝났을 때
     * 실제 값을 DB에 반영하는 리스너입니다. 좌/우 테이블에 각각 하나씩 붙습니다.
     */
    private class CellEditListener implements javax.swing.event.TableModelListener {
        private final DefaultTableModel model;
        private final List<CharacterVO> displayedList; // 이 테이블의 row 번호 -> CharacterVO(char_id 포함)

        CellEditListener(DefaultTableModel model, List<CharacterVO> displayedList) {
            this.model = model;
            this.displayedList = displayedList;
        }

        @Override
        public void tableChanged(javax.swing.event.TableModelEvent e) {
            // revertCell()이 값을 되돌리는 중에 발생한 이벤트는 무시 (무한 루프 방지)
            if (suppressTableEvents) {
                return;
            }
            // 셀 편집으로 인한 값 변경(UPDATE)만 처리. addRow/setRowCount로 인한
            // INSERT/DELETE 구조 변경 이벤트(초기 데이터 채우기 등)는 무시합니다.
            if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) {
                return;
            }

            int row = e.getFirstRow();
            int column = e.getColumn();

            // 캐릭터명(0), 전직(1), 레벨(2) 컬럼만 처리
            if (column != 0 && column != 1 && column != 2) {
                return;
            }
            // 패딩용 빈 행이거나 범위를 벗어난 행은 무시
            if (row < 0 || row >= displayedList.size()) {
                return;
            }

            CharacterVO vo = displayedList.get(row);

            if (column == 2) {
                // ---- 레벨 수정 (스피너에서 전달된 값은 항상 Integer) ----
                int oldLevel = vo.getLevel();
                Object rawValue = model.getValueAt(row, column);
                int newLevel;
                try {
                    newLevel = (rawValue instanceof Integer)
                            ? (Integer) rawValue
                            : Integer.parseInt(String.valueOf(rawValue).trim());
                } catch (NumberFormatException ex) {
                    revertCell(model, row, column, oldLevel);
                    return;
                }

                if (newLevel == oldLevel) {
                    return; // 실제로 바뀐 게 없으면 DB 호출 생략
                }

                vo.setLevel(newLevel);
                dao.updateLevel(vo.getCharId(), newLevel);

                // 레벨이 바뀌었으니 하단 총합도 다시 계산
                int total = dao.sumLevel();
                sumLabel.setText("   |   레벨 총합: " + total);
                return;
            }

            // ---- 캐릭터명(0) / 전직(1) 수정 ----
            String oldValue = (column == 0) ? vo.getCharName() : vo.getJob();
            String newValue = String.valueOf(model.getValueAt(row, column)).trim();

            if (newValue.isEmpty()) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "빈 값으로는 수정할 수 없습니다.",
                        "수정 오류", JOptionPane.ERROR_MESSAGE);
                revertCell(model, row, column, oldValue);
                return;
            }

            if (newValue.equals(oldValue)) {
                return; // 실제로 바뀐 게 없으면 DB 호출 생략
            }

            if (column == 0) {
                vo.setCharName(newValue);
                dao.updateCharName(vo.getCharId(), newValue);
            } else {
                vo.setJob(newValue);
                dao.updateJob(vo.getCharId(), newValue);
            }
        }
    }

    /** 유효성 검사 실패 시, 편집한 셀 값을 이전 값으로 되돌립니다. */
    private void revertCell(DefaultTableModel model, int row, int column, Object oldValue) {
        suppressTableEvents = true;
        model.setValueAt(oldValue, row, column);
        suppressTableEvents = false;
    }

    /**
     * "레벨" 컬럼 전용 편집기입니다. 더블클릭하면 스피너가 나타나서
     * - 스피너 안 텍스트 필드를 클릭해 숫자를 직접 타이핑하거나
     * - 위/아래 화살표 버튼으로 1씩 증감
     * 두 가지 방식으로 모두 수정할 수 있습니다.
     */
    private class LevelSpinnerEditor extends javax.swing.AbstractCellEditor
            implements javax.swing.table.TableCellEditor {

        private final JSpinner spinner;

        // 편집을 시작한 시점의 "이 행을 제외한 나머지 41명의 레벨 합".
        // 여기에 스피너의 현재 값을 더하면 = 지금 화면에 보여줄 미리보기 총합.
        private int baseTotalExcludingThisRow;

        LevelSpinnerEditor() {
            spinner = new JSpinner(new javax.swing.SpinnerNumberModel(1, 1, 300, 1));

            // 스피너 안 텍스트 필드에 포커스가 가면 기존 값이 바로 선택되게 해서
            // 굳이 지우지 않고 바로 새 숫자를 타이핑할 수 있게 합니다.
            javax.swing.JFormattedTextField textField =
                    ((javax.swing.JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    javax.swing.SwingUtilities.invokeLater(textField::selectAll);
                }
            });

            // 화살표 버튼을 누르는 즉시(=편집을 끝내기 전이라도) 하단 "레벨 총합"에
            // 미리보기 값을 바로 반영합니다. (실제 DB 반영은 편집이 끝날 때 CellEditListener가 처리)
            spinner.addChangeListener(e -> {
                int previewValue = (Integer) spinner.getValue();
                sumLabel.setText("   |   레벨 총합: " + (baseTotalExcludingThisRow + previewValue));
            });
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public boolean stopCellEditing() {
            try {
                // 화살표를 안 누르고 타이핑만 한 뒤 바로 다른 곳을 클릭한 경우까지
                // 입력한 값이 확실히 반영되도록 강제로 커밋합니다.
                spinner.commitEdit();
            } catch (java.text.ParseException e) {
                return false; // 숫자가 아닌 값이면 편집을 취소하지 않고 그대로 유지(포커스 유지)
            }
            return super.stopCellEditing();
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            int level = 1;
            if (value instanceof Integer) {
                level = (Integer) value;
            } else if (value != null) {
                try {
                    level = Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException ignore) {
                    // 빈 행(패딩)처럼 숫자가 아니면 기본값(1)을 보여줍니다.
                }
            }

            // 편집 시작 시점의 "현재 이 행을 제외한 총합"을 미리 계산해둡니다.
            // (DB에는 아직 편집 전 값이 저장돼 있으므로 전체 합에서 그 값만 빼면 됩니다)
            baseTotalExcludingThisRow = dao.sumLevel() - level;

            spinner.setValue(level);
            return spinner;
        }
    }

    /**
     * 전체 목록을 왼쪽 테이블(1~21번째)과 오른쪽 테이블(22~42번째)로 나눠 채웁니다.
     * 스크롤 없이 42명을 한 번에 볼 수 있도록, 남는 칸은 빈 행으로 채워
     * 항상 21행 높이를 유지합니다.
     */
    private void fillTable(List<CharacterVO> list) {
        // 주의: leftList/rightList는 CellEditListener가 참조를 들고 있으므로
        // 절대 새 리스트로 재할당(=)하지 않고 항상 clear()+addAll()로만 내용을 갱신합니다.
        leftList.clear();
        leftList.addAll(list.subList(0, Math.min(list.size(), ROWS_PER_TABLE)));

        rightList.clear();
        if (list.size() > ROWS_PER_TABLE) {
            rightList.addAll(list.subList(ROWS_PER_TABLE, list.size()));
        }

        fillSingleTable(leftTableModel, leftList);
        fillSingleTable(rightTableModel, rightList);
    }

    /** 테이블 모델 하나에 데이터를 채우고, 21행이 될 때까지 빈 행으로 패딩합니다. */
    private void fillSingleTable(DefaultTableModel model, List<CharacterVO> list) {
        model.setRowCount(0);
        for (CharacterVO vo : list) {
            model.addRow(new Object[] { vo.getCharName(), vo.getJob(), vo.getLevel() });
        }
        while (model.getRowCount() < ROWS_PER_TABLE) {
            model.addRow(new Object[] { "", "", "" });
        }
    }

    /** 등록된 순서(char_id 오름차순) 그대로 조회하는 간단한 보조 메서드 */
    private List<CharacterVO> selectAllInInsertOrder() {
        List<CharacterVO> list = new java.util.ArrayList<>();
        String sql = "SELECT char_id, char_name, job, level FROM character_info ORDER BY char_id ASC";

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
}