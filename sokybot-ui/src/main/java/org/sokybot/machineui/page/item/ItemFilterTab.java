package org.sokybot.machineui.page.item;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;
import javax.swing.Box;
import info.clearthought.layout.TableLayout;

// Ported from legacy ItemFilterTab
public class ItemFilterTab extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private FilterPane filterPane;
    private ItemTableModel tableModel;
    private TableRowFilter tableRowFilter;
    
    private JTable table;
    private JButton btnSearch;
    private JTextField txtKeyword;
    private TableRowSorter<ItemTableModel> rowSorter;

    public ItemFilterTab() {
        this.filterPane = new FilterPane();
        this.tableModel = new ItemTableModel();
        this.tableRowFilter = new TableRowFilter(filterPane);
        init();
    }

    private void init() {
        this.btnSearch = new JButton("Search");
        this.btnSearch.addActionListener(this);
        this.txtKeyword = new JTextField();
        this.txtKeyword.setPreferredSize(new Dimension(40, this.txtKeyword.getHeight()));

        this.table = new JTable(tableModel);
        this.rowSorter = new TableRowSorter<>(this.tableModel);
        this.rowSorter.setRowFilter(this.tableRowFilter);
        this.table.setRowSorter(rowSorter);

        int border = 5;
        double size[][] = { { border, 0.25, 0.70, border }, // cols
                { border, TableLayout.FILL, TableLayout.PREFERRED, border } // rows
        };

        setLayout(new TableLayout(size));

        add(filterPane, "1 , 1");
        add(new JScrollPane(this.table), "2 ,1");
        add(createTableBox(), " 2 , 2");
    }

    private Box createTableBox() {
        Box line = Box.createHorizontalBox();
        line.add(Box.createHorizontalGlue());
        line.add(this.txtKeyword);
        line.add(this.btnSearch);
        return line;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.rowSorter.sort();
    }
}
