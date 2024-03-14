package org.sokybot.packetsniffer.trafficmonitor;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class TrafficMonitor extends JPanel implements ActionListener {

	private JTable table;
	private JPopupMenu popupMenu;
	private JMenuItem ignoreMenuItem;
	private JMenuItem analyzeMenuItem;
	private JButton btnOperate;

	private JButton btnAnalyze;

	private JButton btnClear;

	private String btnAnalyzeCaption;

	private int selectedRow = 0;

	private IPacketMonitorHandler handler;

	private PacketMonitorTableModel model;

	public TrafficMonitor(PacketMonitorTableModel model) {

		this.model = model;
		table = new JTable(model);

		this.popupMenu = new JPopupMenu();
		this.ignoreMenuItem = new JMenuItem("Ignore");
		this.analyzeMenuItem = new JMenuItem("Analyze");

		this.btnClear = new JButton("Clear");
		this.btnOperate = new JButton("Pasue");

		this.btnAnalyzeCaption = "Analyze(0)";

		this.btnAnalyze = new JButton(btnAnalyzeCaption);
		init();
	}

	private void init() {

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		this.popupMenu.add(this.ignoreMenuItem);
		this.ignoreMenuItem.addActionListener(this);
		this.ignoreMenuItem.setActionCommand(UserAction.IGNORE.toString());

		this.popupMenu.add(this.analyzeMenuItem);
		this.analyzeMenuItem.addActionListener(this);
		this.analyzeMenuItem.setActionCommand(UserAction.ANALYZE.toString());

		this.btnAnalyze.setEnabled(false);

		this.popupMenu.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int rowAtPoint = table
								.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), table));
						if (rowAtPoint > -1) {
							table.setRowSelectionInterval(rowAtPoint, rowAtPoint);
						}
					}
				});
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// TODO Auto-generated method stub

			}
		});
		this.table.setComponentPopupMenu(this.popupMenu);

		JScrollPane tableScroll = new JScrollPane(this.table);
		add(tableScroll, BorderLayout.CENTER);

		this.table.setDefaultRenderer(Object.class, new TrafficMonitorCellRender());
		// this.table.setAutoscrolls(true);
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {

				if (!e.getValueIsAdjusting()) {
					selectedRow = table.getSelectedRow();

					if (handler != null) {
						handler.onSelectPacket(model.getRow(selectedRow));
					}

				}
			}
		});

		this.table.getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {

				tableScroll.getVerticalScrollBar().setValue(tableScroll.getVerticalScrollBar().getMaximum());
				int packetCount = model.getPacketCount() ; 
				
				btnAnalyzeCaption = btnAnalyzeCaption.replaceFirst("(\\d+)",String.valueOf(packetCount)) ; 
				btnAnalyze.setText(btnAnalyzeCaption);
				
				if(packetCount == 0) btnAnalyze.setEnabled(false);
			}
		});

		Box toolBox = Box.createHorizontalBox();

		toolBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		this.btnClear.addActionListener(this);
		this.btnOperate.addActionListener(this);
		this.btnAnalyze.addActionListener(this);

		this.btnClear.setActionCommand(UserAction.CLEAR.toString());
		this.btnOperate.setActionCommand(UserAction.PAUSE.toString());
		this.btnAnalyze.setActionCommand(UserAction.ANALYZE.toString());

		toolBox.add(this.btnClear);
		toolBox.add(this.btnOperate);
		toolBox.add(Box.createHorizontalGlue());
		toolBox.add(this.btnAnalyze);

		add(toolBox, BorderLayout.PAGE_END);

	}

	public void setHandler(IPacketMonitorHandler handler) {
		this.handler = handler;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand() == UserAction.PAUSE.toString()) {
			if (!this.btnAnalyze.isEnabled() && this.model.getPacketCount() > 0) {
				this.btnAnalyze.setEnabled(true);
			}
		} else if (e.getActionCommand() == UserAction.RESUME.toString()) {

			if (this.btnAnalyze.isEnabled()) {
				this.btnAnalyze.setEnabled(true);
			}
		}

		if (this.handler != null)
			this.handler.handle(e);

	}
	
	
}
