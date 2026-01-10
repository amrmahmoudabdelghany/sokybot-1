package org.sokybot.machineui.page.environment;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;

import org.sokybot.machineui.model.MachineViewModel;
import org.sokybot.machineui.page.IMachinePage;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;

public class EnvTab extends JPanel implements IMachinePage {

    private static final long serialVersionUID = 1L;
    private final MachineViewModel viewModel;
    
    private MonsterTable monsterTable;
    private SroMapViewer mv;
    private JTabbedPane infoTab;

    public EnvTab(MachineViewModel viewModel) {
        this.viewModel = viewModel;
        init();
    }

    private void init() {
        this.monsterTable = new MonsterTable(viewModel);
        this.mv = new SroMapViewer(viewModel);

        this.infoTab = new JTabbedPane();
        this.infoTab.putClientProperty(TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB, true);
        this.infoTab.add("Monsters", this.monsterTable);

        int border = 5;
        double sizes[][] = { { border, TableLayout.PREFERRED, TableLayout.FILL, border }, // columns
                { border, TableLayout.FILL, border } // rows
        };

        TableLayout layout = new TableLayout(sizes);
        setLayout(layout);

        add(getMV(), "1 , 1 , C , C");
        add(this.infoTab, "2 , 1 ");
    }

    private JPanel getMV() {
        JPanel border = new JPanel();
        border.setLayout(new BorderLayout());
        border.add(this.mv, BorderLayout.CENTER);
        border.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        return border;
    }

    @Override
    public String getName() {
        return "Environment";
    }

    @Override
    public javax.swing.Icon getIcon() {
        // Return null/default or load icon
        return null;
    }

    @Override
    public javax.swing.JComponent getComponent() {
        return this;
    }
}
