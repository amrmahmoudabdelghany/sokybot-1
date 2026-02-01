package org.sokybot.gameevents.enums;

public enum ItemType {
    UNKNOWN(0x00), Other(0x01), Equipment(0x110),

    // Garment chin
    GarmentHead(0x111), GarmentShoulder(0x112), GarmentBody(0x113), GarmentLegs(0x114), GarmentGloves(0x115),
    GarmentFeet(0x116),
    // Protector chin
    ProtectorHead(0x121), ProtectorShoulder(0x122), ProtectorBody(0x123), ProtectorLegs(0x124), ProtectorGloves(0x125),
    ProtectorFeet(0x126),
    // Armor chin
    ArmorHead(0x131), ArmorShoulder(0x132), ArmorBody(0x133), ArmorLegs(0x134), ArmorGloves(0x135), ArmorFeet(0x136),

    // Shield
    ShieldCH(0x141), ShieldEU(0x142),

    // CH Accessory
    EarringCH(0x151), NecklaceCH(0x152), RingCH(0x153),

    // Robe
    RobeHead(0x191), RobeShoulder(0x192), RobeBody(0x193), RobeLegs(0x194), RobeGloves(0x195), RobeFeet(0x196),
    // LA
    LightArmorHead(0x1A1), LightArmorShoulder(0x1A2), LightArmorBody(0x1A3), LightArmorLegs(0x1A4),
    LightArmorGloves(0x1A5), LightArmorFeet(0x1A6),
    // HA
    HeavyArmorHead(0x1B1), HeavyArmorShoulder(0x1B2), HeavyArmorBody(0x1B3), HeavyArmorLegs(0x1B4),
    HeavyArmorGloves(0x1B5), HeavyArmorFeet(0x1B6),

    // Weapon
    Sword(0x162), Blade(0x163), Spear(0x164), Glaive(0x165), Bow(0x166), SwordOnehander(0x167), SwordTwohander(0x168),
    Axe(0x169), Darkstaff(0x16A), MageStaff(0x16B), Crossbow(0x16C), Dagger(0x16D), Harp(0x16E), ClericStaff(0x16F),
    // Other
    // Trade
    TraderFlag(0x171), ThiefFlag(0x172), HunterFlag(0x173),

    // EU Accessory
    EarringEU(0x1C1), NecklaceEU(0x1C2), RingEU(0x1C3),

    // Itemmall
    AvatarHead(0x1D1), AvatarDress(0x1D2), AvatarAttachment(0x1D3), DevilsSpirit(0x1E1),

    // Pet
    GrowthPet(0x211), AbilityPet(0x212),

    MagicCube(0x231),

    // Consumables
    // Potion
    HPPot(0x311), HPGrain(0x3111), MPPot(0x312), MPGrain(0x3121), VigourPot(0x313), VigourGrain(0x3131),
    PetHPPotion(0x314), GrassOfLife(0x316), HGPPotion(0x319), FortRepair(0x31A),
    // BadStatus
    PurificationPill(0x321), UniversalPill(0x326), PetPill(0x327),

    //
    ReturnScroll(0x331), Vehicle(0x332), ReverseReturnScroll(0x333), StallDecoration(0x334), GlobalChatting(0x335),
    FortMonsterTablet(0x336), GuildMonsterScroll(0x337), FortManual(0x338), FortFlag(0x339),
    EXP_SP_or_Zerk_Scroll(0x33A), FortScroll(0x33B), SkillPointScroll(0x33C), ItemPlusEnhanceScroll(0x33E),

    // Ammo
    Bolt(0x342), Arrow(0x341),

    // Gold
    Gold(0x350), GoldTrade(0x351),

    ItemTrade(0x381), ItemQuest(0x390),

    // Alchemy
    Elixir(0x3A1),

    WeaponElixir(0x3A10), ShieldElixir(0x3A11), ProtectorElixir(0x3A12), AccessoryElixir(0x3A13), LuckyPowder(0x3A2),
    AdvancedElixir(0x3A4),

    // Stones
    MagicStone(0x3B1),

    MagicStoneStrength(0x3B10), MagicStoneIntelligence(0x3B11), MagicStoneMaster(0x3B12), MagicStoneStrikes(0x3B13),
    MagicStoneDiscipline(0x3B14), MagicStonePenetration(0x3B15), MagicStoneDodging(0x3B16), MagicStoneStamina(0x3B17),
    MagicStoneMagic(0x3B18), MagicStoneFogs(0x3B19), MagicStoneAir(0x3B1A), MagicStoneFire(0x3B1B),
    MagicStoneImmunity(0x3B1C), MagicStoneRevival(0x3B1D), MagicStoneSteady(0x3B1E), MagicStoneLuck(0x3B1F),
    MagicStoneAstral(0x3B1F0), MagicStoneImmortal(0x3B1F1),

    AttributeStone(0x3B2),

    AttributeStoneCourage(0x3B20), AttributeStoneWarriors(0x3B21), AttributeStonePhilosophy(0x3B22),
    AttributeStoneMeditation(0x3B23), AttributeStoneChallenge(0x3B24), AttributeStoneFocus(0x3B25),
    AttributeStoneFlesh(0x3B26), AttributeStoneLife(0x3B27), AttributeStoneMind(0x3B28), AttributeStoneSpirit(0x3B29),
    AttributeStoneDodging(0x3B2A), AttributeStoneAgility(0x3B2B), AttributeStoneTraining(0x3B2C),
    AttributeStonePrayer(0x3B2D),

    // Tablets
    AlchemyTablet(0x3B3),

    JadeTabletStrength(0x3B300), JadeTabletIntelligence(0x3B301), JadeTabletMaster(0x3B302), JadeTabletStrikes(0x3B303),
    JadeTabletDiscipline(0x3B304), JadeTabletPenetration(0x3B305), JadeTabletDodging(0x3B306),
    JadeTabletStamina(0x3B307), JadeTabletMagic(0x3B308), JadeTabletFogs(0x3B309), JadeTabletAir(0x3B30A),
    JadeTabletFire(0x3B30B), JadeTabletImmunity(0x3B30C), JadeTabletRevival(0x3B30D), JadeTabletSteady(0x3B30E),
    JadeTabletLuck(0x3B30F), RubyTabletCourage(0x3B310), RubyTabletWarriors(0x3B311), RubyTabletPhilosophy(0x3B312),
    RubyTabletMeditation(0x3B313), RubyTabletChallenge(0x3B314), RubyTabletFocus(0x3B315), RubyTabletFlesh(0x3B316),
    RubyTabletLife(0x3B317), RubyTabletMind(0x3B318), RubyTabletSpirit(0x3B319), RubyTabletDodging(0x3B31A),
    RubyTabletAgility(0x3B31B), RubyTabletTraining(0x3B31C), RubyTabletPrayer(0x3B31D),

    AlchemyMaterial(0x3B4), AlchemyElement(0x3B5), DestructionRondo(0x3B6), MagicStoneMall(0x3B7), // Immortality

    MagicPOP(0x3E1), ItemExchangeCoupon(0x3E2),

    // InventoryExpansionItem ( 0x3D 11), --> 3 3 13 17
    BuffScroll(0x3D1), // INT), STR), Speed
    SpeedPotion(0x3D11), EXPBoostScroll(0x3D4), SPBoostScroll(0x3D5), RepairHammer(0x3D7),
    ItemGenderSwitchScroll(0x3D8), SkinChangeScroll(0x3D9),

    Premium(0x3DE), // including Pet Growth Potion
    PetWatch(0x3DF);

    private int value;

    ItemType(int val) {
        value = val;
    }

    public int getValue() {
        return this.value;
    }

    public static boolean contains(int val) {

        for (ItemType type : values())
            if (type.getValue() == val)
                return true;

        return false;
    }

    public static ItemType parseType(int val, String longId) {

        for (ItemType type : values()) {
            if (type.getValue() == val) {
                switch (type) {
                    case HPPot:
                        return longId.contains("SPOTION") ? ItemType.HPGrain : ItemType.HPPot;
                    case MPPot:
                        return longId.contains("SPOTION") ? ItemType.MPGrain : ItemType.MPPot;
                    case VigourPot:
                        return longId.contains("SPOTION") ? ItemType.VigourGrain : ItemType.VigourPot;
                    case BuffScroll:
                        return longId.contains("POTION_SPEED") ? ItemType.SpeedPotion : ItemType.BuffScroll;
                    case Elixir:
                        return getElixirType(longId);
                    case MagicStone:
                        return getMagicStoneType(longId);
                    case AttributeStone:
                        return getAttributeStoneType(longId);
                    case AlchemyTablet:
                        return getTabletType(longId);
                    case MagicStoneMall:
                        return getMagicStoneType(longId);
                    default:
                        return type;
                }
            }

        }

        return ItemType.UNKNOWN;
    }

    private static ItemType getElixirType(String longID) {

        if (longID.startsWith("ITEM_ETC_ARCHEMY_REINFORCE_RECIPE_WEAPON")) {
            return ItemType.WeaponElixir;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_REINFORCE_RECIPE_SHIELD")) {
            return ItemType.ShieldElixir;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_REINFORCE_RECIPE_ARMOR")) {
            return ItemType.ProtectorElixir;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_REINFORCE_RECIPE_ACCESSARY")) {
            return ItemType.AccessoryElixir;
        } else {
            return ItemType.Elixir;
        }
    }

    private static ItemType getMagicStoneType(String longID) {
        if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_STR")) {
            return ItemType.MagicStoneStrength;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_INT")) {
            return ItemType.MagicStoneIntelligence;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_DUR")) {
            return ItemType.MagicStoneMaster;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_HR")) {
            return ItemType.MagicStoneStrikes;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_EVADE_BLOCK")) {
            return ItemType.MagicStoneDiscipline;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_EVADE_CRITICAL")) {
            return ItemType.MagicStonePenetration;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_ER")) {
            return ItemType.MagicStoneDodging;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_HP")) {
            return ItemType.MagicStoneStamina;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_MP")) {
            return ItemType.MagicStoneMagic;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_FROSTBITE")) {
            return ItemType.MagicStoneFogs;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_ESHOCK")) {
            return ItemType.MagicStoneAir;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_BURN")) {
            return ItemType.MagicStoneFire;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_POISON")) {
            return ItemType.MagicStoneImmunity;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_ZOMBIE")) {
            return ItemType.MagicStoneRevival;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_SOLID")) {
            return ItemType.MagicStoneSteady;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_LUCK")) {
            return ItemType.MagicStoneLuck;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_ASTRAL")) {
            return ItemType.MagicStoneImmortal;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICSTONE_ATHANASIA")) {
            return ItemType.MagicStoneAstral;
        } else {
            return ItemType.MagicStone;
        }
    }

    private static ItemType getAttributeStoneType(String longID) {
        if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_PA")) {
            return ItemType.AttributeStoneCourage;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_PASTR")) {
            return ItemType.AttributeStoneWarriors;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_MA")) {
            return ItemType.AttributeStonePhilosophy;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_MAINT")) {
            return ItemType.AttributeStoneMeditation;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_CRITICAL")) {
            return ItemType.AttributeStoneChallenge;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_HR")) {
            return ItemType.AttributeStoneFocus;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_PD")) {
            return ItemType.AttributeStoneFlesh;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_PDSTR")) {
            return ItemType.AttributeStoneLife;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_MD")) {
            return ItemType.AttributeStoneMind;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_MDINT")) {
            return ItemType.AttributeStoneSpirit;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_ER")) {
            return ItemType.AttributeStoneDodging;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_BR")) {
            return ItemType.AttributeStoneAgility;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_PAR")) {
            return ItemType.AttributeStoneTraining;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRSTONE_MAR")) {
            return ItemType.AttributeStonePrayer;
        } else {
            return ItemType.AttributeStone;
        }
    }

    private static ItemType getTabletType(String longID) {
        if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_STR")) {
            return ItemType.JadeTabletStrength;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_INT")) {
            return ItemType.JadeTabletIntelligence;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_DUR")) {
            return ItemType.JadeTabletMaster;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_HR")) {
            return ItemType.JadeTabletStrikes;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_EVADE_BLOCK")) {
            return ItemType.JadeTabletDiscipline;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_EVADE_CRITICAL")) {
            return ItemType.JadeTabletPenetration;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_ER")) {
            return ItemType.JadeTabletDodging;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_HP")) {
            return ItemType.JadeTabletStamina;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_MP")) {
            return ItemType.JadeTabletMagic;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_FROSTBITE")) {
            return ItemType.JadeTabletFogs;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_ESHOCK")) {
            return ItemType.JadeTabletAir;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_BURN")) {
            return ItemType.JadeTabletFire;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_POISON")) {
            return ItemType.JadeTabletImmunity;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_ZOMBIE")) {
            return ItemType.JadeTabletRevival;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_SOLID")) {
            return ItemType.JadeTabletSteady;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_MAGICTABLET_LUCK")) {
            return ItemType.JadeTabletLuck;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_PA")) {
            return ItemType.RubyTabletCourage;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_PASTR")) {
            return ItemType.RubyTabletWarriors;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_MA")) {
            return ItemType.RubyTabletPhilosophy;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_MAINT")) {
            return ItemType.RubyTabletMeditation;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_CRITICAL")) {
            return ItemType.RubyTabletChallenge;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_HR")) {
            return ItemType.RubyTabletFocus;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_PD")) {
            return ItemType.RubyTabletFlesh;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_PDSTR")) {
            return ItemType.RubyTabletLife;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_MD")) {
            return ItemType.RubyTabletMind;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_MDINT")) {
            return ItemType.RubyTabletSpirit;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_ER")) {
            return ItemType.RubyTabletDodging;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_BR")) {
            return ItemType.RubyTabletAgility;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_PAR")) {
            return ItemType.RubyTabletTraining;
        } else if (longID.startsWith("ITEM_ETC_ARCHEMY_ATTRTABLET_MAR")) {
            return ItemType.RubyTabletPrayer;
        } else {
            return ItemType.AlchemyTablet;
        }
    }

}
