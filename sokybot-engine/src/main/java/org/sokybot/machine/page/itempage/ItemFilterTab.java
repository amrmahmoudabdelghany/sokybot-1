package org.sokybot.machine.page.itempage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdesktop.swingx.sort.RowFilters;
import org.sokybot.machinegroup.gamemodel.Gender;
import org.sokybot.machinegroup.gamemodel.Race;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.ItemType;
import org.sokybot.machinegroup.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.machinegroup.pk2extractor.exception.Pk2MissedResourceException;
import org.sokybot.pk2.IPk2Driver;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarkLaf;

import info.clearthought.layout.TableLayout;

// @Component
public class ItemFilterTab extends JPanel implements ActionListener{

	// @Autowired
	private FilterPane filterPane ; 

	// @Autowired
	private ItemTableModel tableModel ; 
	
	// @Autowired
	private TableRowFilter tableRowFilter ; 
	
	private JTable table ; 
	
	private JButton btnSearch ; 
	private JTextField txtKeyword ; 
	
	private TableRowSorter<ItemTableModel> rowSorter ; 
	
	
	
	
	// @PostConstruct
	private void init() { 
	

		this.btnSearch = new JButton("Search") ; 
		this.btnSearch.addActionListener(this);
		this.txtKeyword = new JTextField() ; 
		this.txtKeyword.setPreferredSize(new Dimension(40 , this.txtKeyword.getHeight()));
		
		
		table = new JTable(tableModel) ; 
		this.rowSorter = new TableRowSorter<ItemTableModel>(this.tableModel) ; 

//		this.rowSorter.setComparator(1, new Comparator<Gender>() {
//			@Override
//			public int compare(Gender o1, Gender o2) {
//			return	o1.name().compareTo(o2.name()) ;
//			//	return o1.compareTo(o2) ;
// 			}
//		});
//		this.rowSorter.setStringConverter(new TableStringConverter() {
//			
//			@Override
//			public String toString(TableModel model, int row, int column) {
//			
//				if(column == 1 ) { 
//					Race race = (Race)	model.getValueAt(row, column);
//					return race.name() ; 
//				}else if(column == 2) { 
//					Gender gender = (Gender) model.getValueAt(row, column) ; 
//					return gender.name() ;
//							
//				}
//				return null;
//			}
//		});
		this.rowSorter.setRowFilter(this.tableRowFilter);
	//	rowSorter.setRowFilter(this.tableRowFilter);
		table.setRowSorter(rowSorter);
		
		int border = 5  ; 
		double size[][] = {{border ,0.25 , 0.70 , border} // cols
		, {border , TableLayout.FILL , TableLayout.PREFERRED , border} // rows
		} ;
		
		setLayout(new TableLayout(size));
		
		add(filterPane , "1 , 1") ; 
		add(new JScrollPane(this.table) , "2 ,1") ;
		add(createTableBox() , " 2 , 2") ; 
		
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		//List<RowFilter<ItemTableModel, Integer>> filters = new ArrayList<>() ; 
		//filters.add(tableRowFilter) ; 
		//filters.add(RowFilters.regexFilter(this.txtKeyword.getText())) ; 
	//	rowSorter.setRowFilter(RowFilter.andFilter(filters));
		
		this.rowSorter.sort();  
	}

	private Box createTableBox() { 
		
		Box line = Box.createHorizontalBox() ; 
		line.add(Box.createHorizontalGlue()) ;
		line.add(this.txtKeyword) ; 
		line.add(this.btnSearch);
		
		
		return line ; 
	}
	
	public static void main(String args[]) {

		FlatDarkLaf.setup();
		JFrame frame = new JFrame();

		JPanel panel = (JPanel) frame.getContentPane();
		FilterPane filterPane = new FilterPane();
		filterPane.init();
		ItemTableModel tableModel = new ItemTableModel() ;
		
		TableRowFilter tableRowFilter = new TableRowFilter(filterPane) ; 
		ItemFilterTab tab = new ItemFilterTab() ; 
		
		tab.filterPane = filterPane ; 
		tab.tableModel = tableModel ; 
		tab.tableRowFilter = tableRowFilter ; 
		tab.init();  
		
         items()
         .forEach((item)->tableModel.add(item));
		//panel.setPreferredSize(new Dimension(200, 400));
		panel.setLayout(new BorderLayout());

		panel.add(tab, BorderLayout.CENTER);

		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}
	
	
	public static List<ItemEntity> items() { 
		
	   IPk2Driver driver = IPk2Driver.open("E:\\Amroo\\Silkroad Games\\RedDiamondSRO_RDSRO_1_042\\RedDiamondSRO_RDSRO_1_042\\Media.pk2") ; 
	   
	   
	   return driver.findFirst("itemdata.txt")
				.map(jmx -> Pk2ExtractorUtils.toString(jmx, StandardCharsets.UTF_16.name()))
				.map(Pk2ExtractorUtils::toLines)
				.orElseThrow(() -> new Pk2MissedResourceException("Colud not find itemdata.txt file", "itemdata.txt"))
				.flatMap((itemFileName) -> driver.find("(?i)" + itemFileName).stream())
				.flatMap(Pk2ExtractorUtils::toCSVRecordStream)
				.filter((record) -> record.size() > 57 && !record.get(0).startsWith("//"))
				.map(ItemFilterTab::toItemEntity)
				.distinct()
				//.peek((itemEntity)->cache.put(itemEntity.getLongId(), itemEntity.getRefId()))
				.collect(Collectors.toList()); 
		
		
		
		
	}
	
	
	private static ItemEntity toItemEntity(CSVRecord record) {
		var builder = ItemEntity.builder();

		String field = record.get(1); // id

		if (NumberUtils.isParsable(field)) {

			builder.refId(Integer.parseInt(field));
		}

		field = record.get(2); // long id

		if (!field.isBlank()) {
			builder.longId(field);
		}

		field = record.get(5); // name
	    String name = 	null;
	     name = (name == null) ? field : name ; 
	     
		//builder.name(this.cacheStorage.getValueOrDefault(field, String.class, field));
	     builder.name(name); 
	     
		field = record.get(7); // isMallItem {0 , 1}
		boolean isMall = false;
		if (NumberUtils.isParsable(field)) {
			isMall = BooleanUtils.toBoolean(Byte.valueOf(field));
		}

		builder.isMallItem(isMall);

		String type = "";

		field = record.get(10); // type byte 1

		if (NumberUtils.isParsable(field)) {
			type += Integer.toHexString(Integer.parseInt(field));
		}

		field = record.get(11); // type byte 2

		if (NumberUtils.isParsable(field)) {
			type += Integer.toHexString(Integer.parseInt(field));
		}

		field = record.get(12); // type byte 3

		if (NumberUtils.isParsable(field)) {
			type += Integer.toHexString(Integer.parseInt(field));
		}

		builder.itemType(ItemType.parseType(Integer.valueOf(type, 16), record.get(2)));

		field = record.get(14); // item race
		Race itemRace = Race.Universal;

		if (NumberUtils.isParsable(field)) {
			itemRace = Race.parseType(Byte.valueOf(field));
		}
		builder.race(itemRace);

		field = record.get(15); // isSOX

		builder.isSOX(field.equals("1"));

		field = record.get(19);
		builder.isSortable(!field.equals("0"));

		field = record.get(33); // lvl
		int lvl = 0;
		if (NumberUtils.isParsable(field)) {
			lvl = Integer.parseInt(field);
		}
		builder.level(lvl);

		field = record.get(57);
		int maxStacks = 0;
		if (NumberUtils.isParsable(field)) {
			maxStacks = Integer.parseInt(field);
		}

		builder.maxStacks(maxStacks);

		Gender itemGender = Gender.Unisex;

		if (record.size() > 58) {
			field = record.get(58);
			if (NumberUtils.isParsable(field)) {
				itemGender = Gender.parseType(Byte.valueOf(field));
			}
		}

		builder.gender(itemGender);

		int degree = 0;

		if (record.size() > 61) {
			field = record.get(61);
			if (NumberUtils.isParsable(field)) {
				degree = (Integer.parseInt(field) + 3 - 1) / 3;
			}
		}

		builder.degree(degree);

		return builder.build();

	}

		
}
