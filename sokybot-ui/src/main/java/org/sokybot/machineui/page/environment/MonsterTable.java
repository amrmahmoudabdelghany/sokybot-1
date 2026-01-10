package org.sokybot.machineui.page.environment;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.sokybot.machine.gamemodel.Trainer;
import org.sokybot.machinegroup.gamemodel.npc.Monster;
import org.sokybot.machineui.model.MachineViewModel;

import info.clearthought.layout.TableLayout;

public class MonsterTable extends JPanel {

    private static final long serialVersionUID = 1L;
    private JTable monsterTable;
    private final MachineViewModel viewModel;
    private MonsterTableModel tableModel;

    public MonsterTable(MachineViewModel viewModel) {
        this.viewModel = viewModel;
        init();
        bind();
    }

    private void init() {
        this.tableModel = new MonsterTableModel();
        this.monsterTable = new JTable(tableModel);
        
        // Handling double click to select?
        this.monsterTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    // int index = table.getSelectedRow();
                    // Select monster logic via engine command?
                    // engine.sendEvent("SELECT_MONSTER:" + id);
                }
            }
        });

        int border = 5;
        double sizes[][] = {
                {border, TableLayout.FILL, border}, 
                {border, TableLayout.FILL, border}
        };
        TableLayout layout = new TableLayout(sizes);
        setLayout(layout);
        this.add(new JScrollPane(monsterTable), "1 , 1");
    }

    private void bind() {
        this.viewModel.addMonsterListener(() -> {
            SwingUtilities.invokeLater(() -> this.tableModel.refresh());
        });
        this.viewModel.addTrainerListener(() -> {
             SwingUtilities.invokeLater(() -> this.tableModel.fireTableDataChanged()); // Refresh distance
        });
    }

    private class MonsterTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private Vector<Monster> monsters = new Vector<>();
        private String[] cols = {"Name", "Type", "Lvl", "HP", "Distance"};

        public void refresh() {
            monsters = new Vector<>(viewModel.getMonsters().values());
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public int getRowCount() {
            return monsters.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= this.monsters.size()) {
                return null;
            }

            Monster monster = this.monsters.get(rowIndex);
            Trainer trainer = viewModel.getTrainer();

            switch (columnIndex) {
                case 0: return monster.getName();
                case 1: return monster.getMonsterType().name();
                case 2: return monster.getLevel();
                case 3: 
                    if (monster.getMaxHP() == 0) return "0%"; // Avoid div by zero
                    return ((long)monster.getCurrentHP() * 100 / monster.getMaxHP()) + "%";
                case 4: 
                    if (trainer != null) {
                        return (int) trainer.distance(monster.getX(), monster.getY());
                    }
                    return 0;
            }
            return "UNKNOWN";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int column) {
            if (column >= this.cols.length) return null;
            return this.cols[column];
        }
    }
}
