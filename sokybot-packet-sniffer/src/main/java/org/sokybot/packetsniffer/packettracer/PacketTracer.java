package org.sokybot.packetsniffer.packettracer;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import org.sokybot.network.NetworkPeer;

public class PacketTracer extends JPanel {

	// private PacketTracerTableModel model ;

	private JTable table;
	private JTextField txtSearch;
	private JTextArea txtDescription;

	private IPacketTracerHandler handler;

	private PacketTracerTableModel model;

	private TableRowSorter<PacketTracerTableModel> sorter;

	public PacketTracer(PacketTracerTableModel model) {

		txtSearch = new JTextField();

		txtDescription = new JTextArea();
		this.table = new JTable(model);

		this.model = model;

		this.sorter = new TableRowSorter<PacketTracerTableModel>(model);
		this.sorter.setRowFilter(RowFilter.regexFilter("."));
		this.table.setRowSorter(sorter);
		this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// filter.setRowFilter(RowFilter.);
		init();
	}

	private void init() {

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		Box searchBox = Box.createHorizontalBox();
		searchBox.add(new JLabel("Search : "));
		searchBox.add(Box.createHorizontalStrut(5));
		searchBox.add(this.txtSearch);
		this.add(searchBox, BorderLayout.PAGE_START);
		searchBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table),
				new JScrollPane(this.txtDescription));
		this.add(split, BorderLayout.CENTER);

		this.txtSearch.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(KeyEvent e) {

				String search = txtSearch.getText();

				if (!search.isBlank()) {
					sorter.setRowFilter(RowFilter.regexFilter(search));
				} else {
					sorter.setRowFilter(RowFilter.regexFilter("."));

				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});

		// this.txtDescription.setRows(5);

		this.txtDescription.addFocusListener(new FocusListener() {

			PacketTracerModel tracer = null;

			@Override
			public void focusLost(FocusEvent e) {

				if (handler != null && tracer != null) {

					tracer.setDescription(txtDescription.getText());
					handler.onTracerChanged(tracer);
				}

			}

			@Override
			public void focusGained(FocusEvent e) {

				if (table.getSelectedRow() != -1) {
					tracer = getSelected();
				}
			}
		});

		this.table.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("tableCellEditor".equals(evt.getPropertyName())) {
					if (!table.isEditing())
						if (handler != null && table.getSelectedRow() != -1) {

							handler.onTracerChanged(getSelected());
						}
				}

			}
		});

		this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {

				if (!e.getValueIsAdjusting()) {

					if (table.getSelectedRow() != -1) {
						PacketTracerModel tracer = getSelected();
						txtDescription.setText(tracer.getDescription());
					} else {
						txtDescription.setText("");
					}
				}
			}
		});
	}

	public void setHandler(IPacketTracerHandler handler) {
		this.handler = handler;
	}

	private PacketTracerModel getSelected() {

		int selectedRow = table.getSelectedRow();
		System.out.println("Selected Row Index : " + selectedRow);
 
		
		String hexOpcode = (String) this.table.getValueAt(selectedRow, 3);

		int opcode = Integer.parseInt(hexOpcode.substring(2), 16);

		NetworkPeer source =(NetworkPeer) this.table.getValueAt(selectedRow, 1); 
		
//		if (sorter.getViewRowCount() != sorter.getModelRowCount()) {
	//		selectedRow = sorter.convertRowIndexToView(selectedRow);
	//		System.out.println("Converted Index : " + selectedRow) ; 
			
//		}

		// return sorter.getModel().getByIndex(selectedRow);
		PacketTracerModel model =  this.model.getTracer(source , opcode);
	//System.out.println("Selected Model : " + model) ; 
		return model ; 
	}

}
