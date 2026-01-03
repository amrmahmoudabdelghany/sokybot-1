package org.sokybot.machine.page.itempage;

import java.util.function.Predicate;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.sokybot.machinegroup.gamemodel.Gender;
import org.sokybot.machinegroup.gamemodel.Race;
import org.sokybot.machinegroup.gamemodel.item.ItemEntity;
import org.sokybot.machinegroup.gamemodel.item.ItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TableRowFilter extends RowFilter<ItemTableModel, Integer> {

	private FilterPane filterPane;

	private final Predicate<ItemEntity> unInterested = (item) -> {

		return !(item.isMallItem() || item.isTradeEquipment());
	};
	private final Predicate<ItemEntity> degreePredicate = (item) -> {

		if (item.getLevel() == 0)
			return true;

		int degree = item.getDegree();
		return degree >= filterPane.getStartDegree() && degree <= filterPane.getEndDegree();
	};

	private final Predicate<ItemEntity> genderPredicate = (item) -> {
		boolean male = filterPane.chkGGMall.isSelected();
		boolean female = filterPane.chkGGFemale.isSelected();
		Gender itemGender = item.getGender();

		return (male && itemGender == Gender.Male) || (female && itemGender == Gender.Female) || (!male && !female)
				|| (itemGender == Gender.Unisex);
	};

	private final Predicate<ItemEntity> racePredicate = (item) -> {
		boolean chin = filterPane.chkGRChinese.isSelected();
		boolean eur = filterPane.chkGREuropean.isSelected();
		Race race = item.getRace();

		return (chin && race == Race.Chinese) || (eur && race == Race.Euro) || (!chin && !eur)
				|| (race == Race.Universal);
	};

	private final Predicate<ItemEntity> clothesTypePredicate = (item) -> {

		if(!(item.isClothing())) return true ; 
		
		boolean isGarment = item.isGarment();
		boolean isProtector = item.isProtector();
		boolean isArmor = item.isArmor();

//		if(item.getLongId().contains("HEAVY") && item.getRace() == Race.Chinese) { 
//			System.out.println("isHeavy : " + isHA) ; 
//			System.out.println("isRob : " + isRob) ; 
//			System.out.println("isLightArrmor : " + isLA) ; 
//			System.out.println("isClothes : " + item.isClothing()) ; 
//		}
//		if (!(isRob || isLA || isHA)) // if the item is not clothes item then accept the item 
//			return true;

		boolean garmentF = filterPane.chkGarment.isSelected();
		boolean protectorF = filterPane.chkProtector.isSelected();
		boolean armorF = filterPane.chkArmor.isSelected();
		boolean headF = filterPane.chkHead.isSelected();
		boolean shoulderF = filterPane.chkShoulder.isSelected();
		boolean chestF = filterPane.chkChest.isSelected();
		boolean bootF = filterPane.chkBoot.isSelected();
		boolean legF = filterPane.chkLeg.isSelected();
		boolean handF = filterPane.chkHand.isSelected();

		
		boolean typeF  = garmentF || protectorF || armorF ; 
		boolean itemF = headF || shoulderF || chestF || bootF || legF || handF ; 
		boolean filterEnabled = typeF || itemF ; 
		
		if(!filterEnabled)  // if no filter selected then reject the item 
			return false ; 
		
		if(typeF) {  // if type filter is enabled 
			
		if(!((garmentF && isGarment) || (protectorF && isProtector) || (armorF && isArmor))) 
			return false ; 
		}
		

		if (!(itemF))
			return true;


		boolean isHead = item.isHead();
		boolean isShoulder = item.isShoulder();
		boolean isChest = item.isChest();
		boolean isBoot = item.isBoot();
		boolean isLeg = item.isLeg();
		boolean isHand = item.isHand();

		return (headF && isHead) || (shoulderF && isShoulder) || (chestF && isChest) || (bootF && isBoot)
				|| (legF && isLeg) || (handF && isHand);

	};

	private final Predicate<ItemEntity> weaponsPredicate = (item) -> {

 
		if(!item.isWeapon()) return true ; 
		
		
		
		 
		switch(item.getItemType()) { 
		case Blade : return this.filterPane.chkWCBlade.isSelected() ;
		case Glaive : return this.filterPane.chkWCGlave.isSelected() ;
		case Spear :  return this.filterPane.chkWCSpear.isSelected() ;
		case Sword : return this.filterPane.chkWCSword.isSelected() ; 
		case Bow : return this.filterPane.chkWCBow.isSelected() ; 
		case SwordOnehander : return this.filterPane.chkWEOneHandSword.isSelected() ; 
		case SwordTwohander : return this.filterPane.chkWETwoHandSword.isSelected() ; 
		case Crossbow : return this.filterPane.chkWEXBow.isSelected() ; 
		case Dagger : return this.filterPane.chkWEDagger.isSelected() ; 
		case Axe : return this.filterPane.chkWEAxe.isSelected() ; 
		case Harp : return this.filterPane.chkWEHarp.isSelected() ; 
		case MageStaff : return this.filterPane.chkWEStaff.isSelected() ; 
		case ClericStaff : return this.filterPane.chkWECRod.isSelected() ; 
		case Darkstaff : return this.filterPane.chkWEWRod.isSelected() ; 
		default:return true ;  
			
		}
		
 
	};

	private final Predicate<ItemEntity> accessoryPredicate = (item) -> {

		
		if(!item.isAccessory()) return true ; 
		
		switch(item.getItemType()) { 
		case RingCH :
		case RingEU : 
		return this.filterPane.chkRing.isSelected() ; 
		case EarringCH : 
		case EarringEU : 
			return this.filterPane.chkEarring.isSelected() ; 
		case NecklaceCH : 
		case NecklaceEU : 
			return this.filterPane.chkNecklace.isSelected() ; 
		default : return true ; 
		}
 
	};

	@Autowired
	public TableRowFilter(FilterPane filterPane) {
		this.filterPane = filterPane;
	}

	@Override
	public boolean include(Entry<? extends ItemTableModel, ? extends Integer> entry) {

		int rowIndex = entry.getIdentifier();

		ItemEntity t = entry.getModel().getRepresentedItem(rowIndex);

		return this.unInterested.test(t) && this.racePredicate.test(t) && this.genderPredicate.test(t)
				&& this.degreePredicate.test(t) && this.clothesTypePredicate.test(t)
				&& this.weaponsPredicate.test(t) && 
				this.accessoryPredicate.test(t);
		
	}

}
