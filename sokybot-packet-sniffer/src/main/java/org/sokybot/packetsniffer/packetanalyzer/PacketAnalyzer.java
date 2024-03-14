package org.sokybot.packetsniffer.packetanalyzer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.packetsniffer.trafficmonitor.TablePacket;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TableView;

public class PacketAnalyzer extends JFrame {

	private int groupLen = 0;

	private JLabel bytesCountLbl;
	private String bytesCount;

	private JLabel matchCountLbl;
	private String matchCount;

	private JTextPane packetPane;
	private JTextPane linePane;
	private JTextPane strPane;

	private PopupListener popupListener;

	private SimpleAttributeSet sourceAttr;
	private SimpleAttributeSet packetIdAttr;
	private SimpleAttributeSet bufferAttr;
	private SimpleAttributeSet boldAttr;

	private DefaultHighlighter packetHighlighter;
	private static List<Object> currentHighlights = new ArrayList<>();

	private JScrollPane lineScroll;
	private JScrollPane strScroll;
	private JScrollPane packetScroll;

	private DefaultStyledDocument packetDoc;
	private DefaultStyledDocument lineDoc;
	private DefaultStyledDocument strDoc;

	private JTable varTable;
	private VarTable varTableModel;

	private TablePacket[] packets;

	private PacketAnalyzer(TablePacket[] packets, VarTable tableModel) {

		this.packets = packets;

		this.matchCount = "0";
		this.matchCountLbl = new JLabel(this.matchCount);

		this.bytesCount = "0 byte(s)";
		this.bytesCountLbl = new JLabel(this.bytesCount);

		this.varTable = new JTable(tableModel);
		this.varTableModel = tableModel;

		this.packetDoc = new DefaultStyledDocument();
		this.lineDoc = new DefaultStyledDocument();
		this.strDoc = new DefaultStyledDocument();

		this.packetPane = new JTextPane(this.packetDoc);
		this.linePane = new JTextPane(this.lineDoc);
		this.strPane = new JTextPane(this.strDoc);

		this.packetHighlighter = new DefaultHighlighter();

		this.packetPane.setAutoscrolls(false);
		this.linePane.setAutoscrolls(false);
		this.strPane.setAutoscrolls(false);

		this.packetIdAttr = new SimpleAttributeSet();
		this.sourceAttr = new SimpleAttributeSet();
		this.bufferAttr = new SimpleAttributeSet();
		this.boldAttr = new SimpleAttributeSet();

		this.lineScroll = new JScrollPane(this.linePane, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		this.strScroll = new JScrollPane(this.strPane, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.packetScroll = new JScrollPane(this.packetPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		this.popupListener = new PopupListener();
		init();
	}

	private void init() {

		Font font = new Font("Consolas", Font.PLAIN, 15);

		this.linePane.setFont(font);
		this.packetPane.setFont(font);
		this.strPane.setFont(font);

		StyleConstants.setBold(this.boldAttr, true);

		this.setLayout(new BorderLayout());

		this.linePane.setEditable(false);
		this.packetPane.setEditable(false);
		this.strPane.setEditable(false);

		JMenuItem defineMenu = new JMenuItem("Define");
		defineMenu.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedBytes = packetPane.getSelectedText();
				if (selectedBytes != null && !selectedBytes.isBlank() && Utils.isHexString(selectedBytes)) {
					String userIn = JOptionPane.showInputDialog("Please enter variable name : ");
					userIn = (userIn == null || userIn.isBlank()) ? "Undefined" : userIn;

					PacketVar var = new PacketVar();

					var.setPacket(getPacketName(packetPane.getSelectionStart()));
					var.setVarName(userIn);
					var.setOriginValue(selectedBytes);
					selectedBytes = "0x"+  selectedBytes.replaceAll("\\s*", "") ; 
					var.setFormatedValue(selectedBytes);
					var.setComment("");
					varTableModel.addPacketVar(var);
				}
			}
		});
		
		
		this.varTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.varTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
			 
				System.out.println("Selected Index : " + varTable.getSelectedRow()) ; 
				if(!e.getValueIsAdjusting()) { 
					PacketVar packetVar = varTableModel.getPacketVar(varTable.getSelectedRow()) ; 
					if(packetVar != null) { 
					
						highlightMatch(packetVar.getOriginValue() );
					}
				}
				
			}
		});

		this.popupListener.addMenuItem(defineMenu);
		this.packetPane.addMouseListener(this.popupListener);

		// 0x950101
		// 0x476072
		this.packetPane.setSelectionColor(new Color(0x476072));
		this.packetPane.setHighlighter(this.packetHighlighter);

		this.lineScroll.setBorder(BorderFactory.createEmptyBorder());
		this.strScroll.setBorder(BorderFactory.createEmptyBorder());
		this.packetScroll.setBorder(BorderFactory.createEmptyBorder());

		this.linePane.setEnabled(false);

		JScrollBar sharedBar = new JScrollBar(JScrollBar.VERTICAL);

		sharedBar.setAutoscrolls(false);
		this.lineScroll.setVerticalScrollBar(sharedBar);
		this.strScroll.setVerticalScrollBar(sharedBar);
		this.packetScroll.setVerticalScrollBar(sharedBar); // must be last in order

		setPreferredSize(new Dimension(400, 500));

		this.packetPane.addCaretListener(new CaretListener() {

			@Override
			public void caretUpdate(CaretEvent e) {

				if (e.getDot() != e.getMark()) {

					String selectedBytes = packetPane.getSelectedText().trim();

					if (selectedBytes != null && Utils.isHexString(selectedBytes)) {

						int byteCount = selectedBytes.replaceAll("(\\s+)", "").length() / 2;

						bytesCount = bytesCount.replaceAll("^[^\\d]*(\\d+)", String.valueOf(byteCount));
						bytesCountLbl.setText(bytesCount);

						// String asciiRep = toAscii(toByteArray(selectedBytes), 0,
						// selectedBytes.length() ) ;

						//highlightMatch(packetPane.getSelectedText() , packetPane.getSelectionEnd() + 1);

						highlightMatchLikeSelected() ; 
						
						System.out.println("Selected bytes : " + selectedBytes + " is valid hex string");
					} else {
						System.out.println("Selected bytes : " + selectedBytes + " is not valid hex string");
						// set counter to 0
						bytesCount = bytesCount.replaceAll("^[^\\d]*(\\d+)", 0 + "");
						bytesCountLbl.setText(bytesCount);

						matchCount = matchCount.replaceAll("^[^\\d]*(\\d+)", 0 + "");
						matchCountLbl.setText(matchCount);
						// remove all highliters

						for (Object o : currentHighlights) {
							packetHighlighter.removeHighlight(o);
						}

					}

				}
			}
		});

		this.addComponentListener(new ComponentAdapter() {

			boolean ready = false;

			@Override
			public void componentShown(ComponentEvent e) {

				ready = true;
				super.componentShown(e);
			}

			@Override
			public void componentResized(ComponentEvent e) {

				if (ready) {
					System.out.println("On Commponent Resized ");

					updateContent();
				}
				super.componentResized(e);
			}
		});
		Box counter = Box.createHorizontalBox();
		counter.add(new JLabel("Selected : "));
		counter.add(this.bytesCountLbl);
		counter.add(Box.createHorizontalStrut(10));
		counter.add(new JLabel("Matches : "));
		counter.add(this.matchCountLbl);
		counter.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		add(counter, BorderLayout.PAGE_START);

		JPanel txtBox = new JPanel(new BorderLayout());

		txtBox.add(this.lineScroll, BorderLayout.LINE_START);
		txtBox.add(this.packetScroll, BorderLayout.CENTER);
		this.packetScroll.setMinimumSize(new Dimension(250, 300));
		txtBox.add(this.strScroll, BorderLayout.LINE_END);

		txtBox.setBorder(BorderFactory.createEtchedBorder());
		txtBox.setMinimumSize(new Dimension(0, 250));
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, txtBox, new JScrollPane(this.varTable));

		split.setDividerSize(2);

		add(split, BorderLayout.CENTER);

	}

	private String getPacketName(int index) {

		if (index < 0)
			return null;

		try {

			

			while (index != -1) {
				String prevLine = "";
				char c;
				while ((c = (this.packetDoc.getText(index, 1).charAt(0))) != '\n') {
					prevLine = c + prevLine ;	
					index -- ; 
					
				}
				
				if(prevLine.contains("[")) { 
				
				  return prevLine ; 	
				}else { 
					index -- ; 
				}
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private void updateContent() {

		Font font = packetPane.getFont();

		int lineCap = packetPane.getBounds().width / ((font.getSize() * 3));

		System.out.println("Line Cap : " + lineCap);

		if (this.groupLen != lineCap) {
			this.groupLen = lineCap;

			this.packetPane.setText("");
			;
			this.linePane.setText("");
			this.strPane.setText("");

			for (int i = 0; i < this.packets.length; i++)
				this.printPacket(packets[i]);
		}
	}

	private void highlightMatchLikeSelected() { 


		System.out.println("ON Highligth");
		for (Object o : currentHighlights) {
			packetHighlighter.removeHighlight(o);
		}
		try {
			String strCont = this.packetDoc.getText(0, this.packetDoc.getLength());
	
			int matchesCount = 0;
			String selectedBytes = packetPane.getSelectedText().replaceAll("(?:([A-Fa-f0-9]{2})\\s*)", "$1 *?\n?");
			Matcher matcher = Pattern.compile(selectedBytes).matcher(strCont) ; 
			
			while(matcher.find()) { 
				int start = matcher.start() ; 
				int end = matcher.end() ; 
				
				if(start != this.packetPane.getSelectionStart() && end != this.packetPane.getSelectionEnd()) { 
					Object o = packetHighlighter.addHighlight(start, end,
							new DefaultHighlightPainter(new Color(0x548CA8)));
					currentHighlights.add(o);
					matchesCount++;
				}
			}

			this.matchCount = this.matchCount.replaceAll("^[^\\d]*(\\d+)", matchesCount + "");
			this.matchCountLbl.setText(this.matchCount);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void highlightMatch(String selectedBytes ) {

		System.out.println("ON Highligth");
		for (Object o : currentHighlights) {
			packetHighlighter.removeHighlight(o);
		}
		try {
			String strCont = this.packetDoc.getText(0, this.packetDoc.getLength());

			int index = 0;
			int matchesCount = 0;
			//selectedBytes = selectedBytes.trim();
			while ((index = strCont.indexOf(selectedBytes, index)) != -1) {

				int endIndex = index + selectedBytes.length() ;

					Object o = packetHighlighter.addHighlight(index, endIndex,
							new DefaultHighlightPainter(new Color(0x548CA8)));
					currentHighlights.add(o);


				index = endIndex;
				matchesCount++;
			}

			this.matchCount = this.matchCount.replaceAll("^[^\\d]*(\\d+)", matchesCount + "");
			this.matchCountLbl.setText(this.matchCount);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void append(DefaultStyledDocument doc, String text, AttributeSet attrs) {
		int offset = doc.getEndPosition().getOffset() - 1;

		try {
			doc.insertString(offset, text, attrs);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void appendLine(DefaultStyledDocument doc, String text, AttributeSet attrs) {
		append(doc, text + "\n", attrs);
	}

	public void printPacket(TablePacket packet) {

		appendLine(this.lineDoc, "Source", this.boldAttr);

		NetworkPeer source = packet.getPacket().getPacketSource();

		if (source == NetworkPeer.SERVER) {
			StyleConstants.setForeground(this.sourceAttr, new Color(0xECAC5D));
		} else if (source == NetworkPeer.CLIENT || source == NetworkPeer.BOT) {
			StyleConstants.setForeground(this.sourceAttr, new Color(178, 64, 128));

		}

		appendLine(this.packetDoc, source.toString(), this.sourceAttr);
		appendLine(this.strDoc, "", null);

		/////////////////////

		appendLine(this.lineDoc, "Opcode", this.boldAttr);

		StyleConstants.setItalic(this.packetIdAttr, true);
		StyleConstants.setForeground(this.packetIdAttr, new Color(0x5D8233));
		appendLine(this.packetDoc,
				packet.getName() + "[0x" + Integer.toHexString(packet.getPacket().getOpcode() & 0xffff) + "]",
				this.packetIdAttr);

		appendLine(this.strDoc, "", null);

		append(this.lineDoc, "\n", null);
		append(this.packetDoc, "\n", null);
		append(this.strDoc, "\n", null);

		int bs = this.groupLen; // (width - 8) / 4;

		int i = 0;
		byte[] buffer = packet.getPacket().getPacketReader().readBytes(0);

		do {

			String lineNumber = "";
			for (int j = 0; j < 6; j++) {
				lineNumber += (Utils.hexchars[(i << (j * 4) & 0xF00000) >> 20]);
			}

			appendLine(this.lineDoc, lineNumber, null);

			StyleConstants.setForeground(this.bufferAttr, new Color(0xF5F7B2));
			appendLine(this.packetDoc, Utils.toHex(buffer, i, bs), this.bufferAttr);

			appendLine(this.strDoc, Utils.toAscii(buffer, i, bs), null);

			i += bs;
		} while (i < buffer.length);

		appendLine(this.lineDoc, "", null);
		appendLine(this.packetDoc, "", null);
		appendLine(this.strDoc, "", null);

	}

	public void display() {

		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.pack();
		updateContent();
		this.setVisible(true);

	}

	public static void showPacketAnalyzer(TablePacket[] packets, List<PacketVar> vars) {

		PacketAnalyzer analyzer = new PacketAnalyzer(packets, new VarTable(vars));

		analyzer.display();

	}

	public static void main(String args[]) {

		//FlatDarkLaf.setup();

		byte[] arr = new byte[] { 0x34, 0x0E, (byte) 0xFD, 0x00, 0x01, (byte) 0x89, 0x5C, (byte) 0x91, (byte) 0xFE,
				0x0B, 0x00, (byte) 0xE3, 0x01, 0x01, (byte) 0x89, 0x5C, 0x0C, 0x03, 0x63, 0x15, 0x35, 0x41, 0x5C,
				0x12 };

		ImmutablePacket packet = ImmutablePacket.wrap(arr);

		TablePacket tp[] = new TablePacket[5];

		for (int i = 0; i < tp.length; i++) {

			tp[i] = TablePacket.createTablePacket("UNKOWN", packet);
		}

		PacketAnalyzer.showPacketAnalyzer(tp, new ArrayList<>());
		/// frame.pack();
		// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// frame.setVisible(true);

	}
}
