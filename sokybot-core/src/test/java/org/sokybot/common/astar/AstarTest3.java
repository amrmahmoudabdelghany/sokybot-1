package org.sokybot.common.astar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.sokybot.machinegroup.MachineGroupConfig;
import org.sokybot.machinegroup.gamemodel.geo.Vector2D;
import org.sokybot.machinegroup.gamemodel.navmesh.Position;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.machinegroup.service.ISroMaterialDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = "gamePath=E:/Amroo/Silkroad Games/LegionSRO_15_08_2019" )
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = {MachineGroupConfig.class , TestConfig.class})
@TestPropertySource(locations = {"/application.properties"})
@ActiveProfiles(value = {"test" , "dev"} )
//@Profile("test")
public class AstarTest3 {


	
	@Autowired
	private ISroMaterialDAO dao ; 
	

	private RuteFinder s  ; 
	
	
	@PostConstruct
	private void init() { 
	s	= new RuteFinder(dao) ;
	}
	
	
	@Test
	public void test5356$667_2896$667_5424_2874(){
	 List<Vector2D> p = s.findPath(-5356.667f,2896.667f,-5424f,2874f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(-5399.687,path[0].x);
	assertEquals(2860.604,path[0].y);
	assertEquals(-5401.731,path[1].x);
	assertEquals(2877.427,path[1].y);
	assertEquals(-5402.808,path[2].x);
	assertEquals(2878.396,path[2].y);
	assertEquals(-5402.808,path[3].x);
	assertEquals(2879.877,path[3].y);
	assertEquals(-5402.683,path[4].x);
	assertEquals(2880,path[4].y);
	assertEquals(-5402.683,path[5].x);
	assertEquals(2880,path[5].y);
	assertEquals(-5401.691,path[6].x);
	assertEquals(2883.878,path[6].y);
	assertEquals(-5387.383,path[7].x);
	assertEquals(2892.248,path[7].y);
	assertEquals(-5382.691,path[8].x);
	assertEquals(2892.226,path[8].y);
	assertEquals(-5378,path[9].x);
	assertEquals(2892.248,path[9].y);
	assertEquals(-5378,path[10].x);
	assertEquals(2892.248,path[10].y);
	assertEquals(-5377,path[11].x);
	assertEquals(2892.252,path[11].y);
	assertEquals(-5376,path[12].x);
	assertEquals(2892.248,path[12].y);
	assertEquals(-5376,path[13].x);
	assertEquals(2892.248,path[13].y);
	assertEquals(-5371.232,path[14].x);
	assertEquals(2892.226,path[14].y);
	assertEquals(-5366.464,path[15].x);
	assertEquals(2892.248,path[15].y);
	assertEquals(-5361.723,path[16].x);
	assertEquals(2895.373,path[16].y);
	}
	
	@Test
	public void test4068_1555$333_4226_1392(){
	 List<Vector2D> p = s.findPath(4068f,1555.333f,4226f,1392f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4224,path[0].x);
	assertEquals(1368,path[0].y);
	assertEquals(4224,path[1].x);
	assertEquals(1368,path[1].y);
	assertEquals(4174,path[2].x);
	assertEquals(1358,path[2].y);
	assertEquals(4144,path[3].x);
	assertEquals(1352,path[3].y);
	assertEquals(4136,path[4].x);
	assertEquals(1349,path[4].y);
	assertEquals(4134,path[5].x);
	assertEquals(1348,path[5].y);
	assertEquals(4118,path[6].x);
	assertEquals(1346,path[6].y);
	assertEquals(4107,path[7].x);
	assertEquals(1350,path[7].y);
	assertEquals(4104,path[8].x);
	assertEquals(1351,path[8].y);
	assertEquals(4090,path[9].x);
	assertEquals(1354,path[9].y);
	assertEquals(4088,path[10].x);
	assertEquals(1355,path[10].y);
	assertEquals(4081,path[11].x);
	assertEquals(1358,path[11].y);
	assertEquals(4079,path[12].x);
	assertEquals(1360,path[12].y);
	assertEquals(4078,path[13].x);
	assertEquals(1361,path[13].y);
	assertEquals(4072,path[14].x);
	assertEquals(1364,path[14].y);
	assertEquals(4070,path[15].x);
	assertEquals(1365,path[15].y);
	assertEquals(4066,path[16].x);
	assertEquals(1370,path[16].y);
	assertEquals(4065,path[17].x);
	assertEquals(1372,path[17].y);
	assertEquals(4064,path[18].x);
	assertEquals(1374,path[18].y);
	assertEquals(4059,path[19].x);
	assertEquals(1378,path[19].y);
	assertEquals(4058,path[20].x);
	assertEquals(1379,path[20].y);
	assertEquals(4055,path[21].x);
	assertEquals(1382,path[21].y);
	assertEquals(4054,path[22].x);
	assertEquals(1386,path[22].y);
	assertEquals(4041,path[23].x);
	assertEquals(1392,path[23].y);
	assertEquals(4032,path[24].x);
	assertEquals(1400,path[24].y);
	assertEquals(4030,path[25].x);
	assertEquals(1405,path[25].y);
	assertEquals(4030,path[26].x);
	assertEquals(1405,path[26].y);
	assertEquals(4021.751,path[27].x);
	assertEquals(1416.015,path[27].y);
	assertEquals(4021.745,path[28].x);
	assertEquals(1416.892,path[28].y);
	assertEquals(4016.745,path[29].x);
	assertEquals(1417.892,path[29].y);
	assertEquals(4015.609,path[30].x);
	assertEquals(1418.993,path[30].y);
	assertEquals(4017,path[31].x);
	assertEquals(1430,path[31].y);
	assertEquals(4017,path[32].x);
	assertEquals(1434,path[32].y);
	assertEquals(4010,path[33].x);
	assertEquals(1439,path[33].y);
	assertEquals(4008,path[34].x);
	assertEquals(1439,path[34].y);
	assertEquals(4004,path[35].x);
	assertEquals(1446,path[35].y);
	assertEquals(4002,path[36].x);
	assertEquals(1447,path[36].y);
	assertEquals(3999,path[37].x);
	assertEquals(1458,path[37].y);
	assertEquals(3998,path[38].x);
	assertEquals(1463,path[38].y);
	assertEquals(3998,path[39].x);
	assertEquals(1481,path[39].y);
	assertEquals(3999,path[40].x);
	assertEquals(1486,path[40].y);
	assertEquals(3999,path[41].x);
	assertEquals(1486,path[41].y);
	assertEquals(4005,path[42].x);
	assertEquals(1493,path[42].y);
	assertEquals(3999.303,path[43].x);
	assertEquals(1502.025,path[43].y);
	assertEquals(4000.274,path[44].x);
	assertEquals(1505.753,path[44].y);
	assertEquals(4001.542,path[45].x);
	assertEquals(1506.848,path[45].y);
	assertEquals(4001.542,path[46].x);
	assertEquals(1507.848,path[46].y);
	assertEquals(4003.645,path[47].x);
	assertEquals(1508.907,path[47].y);
	assertEquals(4004.911,path[48].x);
	assertEquals(1509.067,path[48].y);
	assertEquals(4005.431,path[49].x);
	assertEquals(1509.876,path[49].y);
	assertEquals(4012,path[50].x);
	assertEquals(1505,path[50].y);
	assertEquals(4014,path[51].x);
	assertEquals(1507,path[51].y);
	assertEquals(4015,path[52].x);
	assertEquals(1508,path[52].y);
	assertEquals(4018,path[53].x);
	assertEquals(1511,path[53].y);
	assertEquals(4019,path[54].x);
	assertEquals(1512,path[54].y);
	assertEquals(4022,path[55].x);
	assertEquals(1515,path[55].y);
	assertEquals(4024,path[56].x);
	assertEquals(1516,path[56].y);
	assertEquals(4030,path[57].x);
	assertEquals(1527,path[57].y);
	assertEquals(4032,path[58].x);
	assertEquals(1530,path[58].y);
	assertEquals(4037,path[59].x);
	assertEquals(1536,path[59].y);
	assertEquals(4050,path[60].x);
	assertEquals(1544,path[60].y);
	}
	
	@Test
	public void test4072_1678_4558$667_1394(){
	 List<Vector2D> p = s.findPath(4072f,1678f,4558.667f,1394f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4546.381,path[0].x);
	assertEquals(1397.879,path[0].y);
	assertEquals(4530,path[1].x);
	assertEquals(1403,path[1].y);
	assertEquals(4530,path[2].x);
	assertEquals(1403,path[2].y);
	assertEquals(4518.784,path[3].x);
	assertEquals(1403.426,path[3].y);
	assertEquals(4513.261,path[4].x);
	assertEquals(1405.957,path[4].y);
	assertEquals(4511,path[5].x);
	assertEquals(1422,path[5].y);
	assertEquals(4492,path[6].x);
	assertEquals(1441,path[6].y);
	assertEquals(4454,path[7].x);
	assertEquals(1441,path[7].y);
	assertEquals(4434,path[8].x);
	assertEquals(1460,path[8].y);
	assertEquals(4416,path[9].x);
	assertEquals(1466,path[9].y);
	assertEquals(4387,path[10].x);
	assertEquals(1472,path[10].y);
	assertEquals(4366,path[11].x);
	assertEquals(1492,path[11].y);
	assertEquals(4325,path[12].x);
	assertEquals(1536,path[12].y);
	assertEquals(4322,path[13].x);
	assertEquals(1539,path[13].y);
	assertEquals(4319,path[14].x);
	assertEquals(1546,path[14].y);
	assertEquals(4318,path[15].x);
	assertEquals(1547,path[15].y);
	assertEquals(4314,path[16].x);
	assertEquals(1549,path[16].y);
	assertEquals(4302,path[17].x);
	assertEquals(1558,path[17].y);
	assertEquals(4300,path[18].x);
	assertEquals(1560,path[18].y);
	assertEquals(4295,path[19].x);
	assertEquals(1568,path[19].y);
	assertEquals(4295,path[20].x);
	assertEquals(1582,path[20].y);
	assertEquals(4295,path[21].x);
	assertEquals(1582,path[21].y);
	assertEquals(4297,path[22].x);
	assertEquals(1587,path[22].y);
	assertEquals(4295,path[23].x);
	assertEquals(1595,path[23].y);
	assertEquals(4295,path[24].x);
	assertEquals(1596,path[24].y);
	assertEquals(4295,path[25].x);
	assertEquals(1598,path[25].y);
	assertEquals(4295,path[26].x);
	assertEquals(1598,path[26].y);
	assertEquals(4295.569,path[27].x);
	assertEquals(1604.03,path[27].y);
	assertEquals(4294.604,path[28].x);
	assertEquals(1605.267,path[28].y);
	assertEquals(4288,path[29].x);
	assertEquals(1613,path[29].y);
	assertEquals(4288,path[30].x);
	assertEquals(1613,path[30].y);
	assertEquals(4264.708,path[31].x);
	assertEquals(1619.643,path[31].y);
	assertEquals(4264.634,path[32].x);
	assertEquals(1621.849,path[32].y);
	assertEquals(4264.634,path[33].x);
	assertEquals(1629.849,path[33].y);
	assertEquals(4263.127,path[34].x);
	assertEquals(1631.191,path[34].y);
	assertEquals(4231.127,path[35].x);
	assertEquals(1631.191,path[35].y);
	assertEquals(4224,path[36].x);
	assertEquals(1639,path[36].y);
	assertEquals(4224,path[37].x);
	assertEquals(1639,path[37].y);
	assertEquals(4221,path[38].x);
	assertEquals(1639,path[38].y);
	assertEquals(4219,path[39].x);
	assertEquals(1639,path[39].y);
	assertEquals(4216,path[40].x);
	assertEquals(1639,path[40].y);
	assertEquals(4208,path[41].x);
	assertEquals(1653,path[41].y);
	assertEquals(4208,path[42].x);
	assertEquals(1653,path[42].y);
	assertEquals(4202.318,path[43].x);
	assertEquals(1648.551,path[43].y);
	assertEquals(4201.616,path[44].x);
	assertEquals(1647.836,path[44].y);
	assertEquals(4201.43,path[45].x);
	assertEquals(1647.666,path[45].y);
	assertEquals(4199.721,path[46].x);
	assertEquals(1646.848,path[46].y);
	assertEquals(4185.637,path[47].x);
	assertEquals(1644.732,path[47].y);
	assertEquals(4184.953,path[48].x);
	assertEquals(1645.507,path[48].y);
	assertEquals(4182.745,path[49].x);
	assertEquals(1645.117,path[49].y);
	assertEquals(4182.628,path[50].x);
	assertEquals(1645.189,path[50].y);
	assertEquals(4182.148,path[51].x);
	assertEquals(1645.451,path[51].y);
	assertEquals(4179.971,path[52].x);
	assertEquals(1646.404,path[52].y);
	assertEquals(4179.792,path[53].x);
	assertEquals(1646.411,path[53].y);
	assertEquals(4178.816,path[54].x);
	assertEquals(1646.524,path[54].y);
	assertEquals(4178.383,path[55].x);
	assertEquals(1646.47,path[55].y);
	assertEquals(4171.046,path[56].x);
	assertEquals(1652.06,path[56].y);
	assertEquals(4168.046,path[57].x);
	assertEquals(1647.06,path[57].y);
	assertEquals(4162,path[58].x);
	assertEquals(1644,path[58].y);
	assertEquals(4156,path[59].x);
	assertEquals(1644,path[59].y);
	assertEquals(4156,path[60].x);
	assertEquals(1644,path[60].y);
	assertEquals(4150,path[61].x);
	assertEquals(1645,path[61].y);
	assertEquals(4132.939,path[62].x);
	assertEquals(1653.488,path[62].y);
	assertEquals(4130.939,path[63].x);
	assertEquals(1653.488,path[63].y);
	assertEquals(4123.939,path[64].x);
	assertEquals(1653.488,path[64].y);
	assertEquals(4122.902,path[65].x);
	assertEquals(1654.529,path[65].y);
	assertEquals(4119.902,path[66].x);
	assertEquals(1654.529,path[66].y);
	assertEquals(4118.932,path[67].x);
	assertEquals(1654.575,path[67].y);
	assertEquals(4117.932,path[68].x);
	assertEquals(1654.575,path[68].y);
	assertEquals(4115.932,path[69].x);
	assertEquals(1654.575,path[69].y);
	assertEquals(4114.932,path[70].x);
	assertEquals(1654.575,path[70].y);
	assertEquals(4111.359,path[71].x);
	assertEquals(1653.21,path[71].y);
	assertEquals(4108.02,path[72].x);
	assertEquals(1654.165,path[72].y);
	assertEquals(4107.887,path[73].x);
	assertEquals(1655.151,path[73].y);
	assertEquals(4105.887,path[74].x);
	assertEquals(1655.151,path[74].y);
	assertEquals(4105.158,path[75].x);
	assertEquals(1656.111,path[75].y);
	assertEquals(4103.158,path[76].x);
	assertEquals(1656.111,path[76].y);
	assertEquals(4102.336,path[77].x);
	assertEquals(1655.988,path[77].y);
	assertEquals(4097.336,path[78].x);
	assertEquals(1655.988,path[78].y);
	assertEquals(4096.353,path[79].x);
	assertEquals(1655.385,path[79].y);
	assertEquals(4094,path[80].x);
	assertEquals(1654,path[80].y);
	assertEquals(4094,path[81].x);
	assertEquals(1654,path[81].y);
	assertEquals(4080.687,path[82].x);
	assertEquals(1658.366,path[82].y);
	assertEquals(4080.382,path[83].x);
	assertEquals(1660.199,path[83].y);
	assertEquals(4080.188,path[84].x);
	assertEquals(1660.573,path[84].y);
	assertEquals(4074.188,path[85].x);
	assertEquals(1669.573,path[85].y);
	assertEquals(4073.812,path[86].x);
	assertEquals(1669.814,path[86].y);
	}
	@Test
	public void test4254$667_1654$667_4521$333_1482(){
	 List<Vector2D> p = s.findPath(4254.667f,1654.667f,4521.333f,1482f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4515,path[0].x);
	assertEquals(1479,path[0].y);
	assertEquals(4496,path[1].x);
	assertEquals(1479,path[1].y);
	assertEquals(4492,path[2].x);
	assertEquals(1479,path[2].y);
	assertEquals(4454,path[3].x);
	assertEquals(1476,path[3].y);
	assertEquals(4452,path[4].x);
	assertEquals(1476,path[4].y);
	assertEquals(4416,path[5].x);
	assertEquals(1466,path[5].y);
	assertEquals(4359,path[6].x);
	assertEquals(1472,path[6].y);
	assertEquals(4325,path[7].x);
	assertEquals(1536,path[7].y);
	assertEquals(4322,path[8].x);
	assertEquals(1539,path[8].y);
	assertEquals(4319,path[9].x);
	assertEquals(1546,path[9].y);
	assertEquals(4318,path[10].x);
	assertEquals(1547,path[10].y);
	assertEquals(4314,path[11].x);
	assertEquals(1549,path[11].y);
	assertEquals(4302,path[12].x);
	assertEquals(1558,path[12].y);
	assertEquals(4301,path[13].x);
	assertEquals(1562,path[13].y);
	assertEquals(4300,path[14].x);
	assertEquals(1563,path[14].y);
	assertEquals(4295,path[15].x);
	assertEquals(1568,path[15].y);
	assertEquals(4295,path[16].x);
	assertEquals(1582,path[16].y);
	assertEquals(4295,path[17].x);
	assertEquals(1582,path[17].y);
	assertEquals(4297,path[18].x);
	assertEquals(1587,path[18].y);
	assertEquals(4295,path[19].x);
	assertEquals(1595,path[19].y);
	assertEquals(4295,path[20].x);
	assertEquals(1596,path[20].y);
	assertEquals(4295,path[21].x);
	assertEquals(1598,path[21].y);
	assertEquals(4295,path[22].x);
	assertEquals(1598,path[22].y);
	assertEquals(4295.569,path[23].x);
	assertEquals(1604.03,path[23].y);
	assertEquals(4294.604,path[24].x);
	assertEquals(1605.267,path[24].y);
	assertEquals(4288,path[25].x);
	assertEquals(1613,path[25].y);
	assertEquals(4288,path[26].x);
	assertEquals(1613,path[26].y);
	assertEquals(4264.708,path[27].x);
	assertEquals(1619.643,path[27].y);
	assertEquals(4264.634,path[28].x);
	assertEquals(1621.849,path[28].y);
	assertEquals(4264.634,path[29].x);
	assertEquals(1629.849,path[29].y);
	assertEquals(4263.127,path[30].x);
	assertEquals(1631.191,path[30].y);
	assertEquals(4256,path[31].x);
	assertEquals(1644,path[31].y);
	assertEquals(4256,path[32].x);
	assertEquals(1644,path[32].y);
	}
	
	
	@Test
	public void test4337$333_1524$667_4406$667_1619$333(){
	 List<Vector2D> p = s.findPath(4337.333f,1524.667f,4406.667f,1619.333f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4401,path[0].x);
	assertEquals(1616,path[0].y);
	assertEquals(4401,path[1].x);
	assertEquals(1616,path[1].y);
	assertEquals(4390,path[2].x);
	assertEquals(1592,path[2].y);
	assertEquals(4385,path[3].x);
	assertEquals(1578,path[3].y);
	assertEquals(4372,path[4].x);
	assertEquals(1564,path[4].y);
	assertEquals(4370,path[5].x);
	assertEquals(1562,path[5].y);
	assertEquals(4368,path[6].x);
	assertEquals(1559,path[6].y);
	assertEquals(4367,path[7].x);
	assertEquals(1558,path[7].y);
	assertEquals(4364,path[8].x);
	assertEquals(1554,path[8].y);
	assertEquals(4363,path[9].x);
	assertEquals(1552,path[9].y);
	assertEquals(4360,path[10].x);
	assertEquals(1549,path[10].y);
	assertEquals(4359,path[11].x);
	assertEquals(1548,path[11].y);
	assertEquals(4351,path[12].x);
	assertEquals(1536,path[12].y);
	}
	
	
	@Test
	public void test4549$333_1474_4505$333_1688$667(){
	 List<Vector2D> p = s.findPath(4549.333f,1474f,4505.333f,1688.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4506,path[0].x);
	assertEquals(1674,path[0].y);
	assertEquals(4506,path[1].x);
	assertEquals(1674,path[1].y);
	assertEquals(4520,path[2].x);
	assertEquals(1648,path[2].y);
	assertEquals(4525,path[3].x);
	assertEquals(1644,path[3].y);
	assertEquals(4525,path[4].x);
	assertEquals(1644,path[4].y);
	assertEquals(4525,path[5].x);
	assertEquals(1641,path[5].y);
	assertEquals(4525,path[6].x);
	assertEquals(1638,path[6].y);
	assertEquals(4529,path[7].x);
	assertEquals(1632,path[7].y);
	assertEquals(4529,path[8].x);
	assertEquals(1632,path[8].y);
	assertEquals(4530,path[9].x);
	assertEquals(1630,path[9].y);
	assertEquals(4540,path[10].x);
	assertEquals(1620,path[10].y);
	assertEquals(4540,path[11].x);
	assertEquals(1620,path[11].y);
	assertEquals(4541,path[12].x);
	assertEquals(1620,path[12].y);
	assertEquals(4543,path[13].x);
	assertEquals(1612,path[13].y);
	assertEquals(4543,path[14].x);
	assertEquals(1604,path[14].y);
	assertEquals(4546,path[15].x);
	assertEquals(1580,path[15].y);
	assertEquals(4550,path[16].x);
	assertEquals(1550,path[16].y);
	assertEquals(4552,path[17].x);
	assertEquals(1536,path[17].y);
	assertEquals(4552,path[18].x);
	assertEquals(1530,path[18].y);
	assertEquals(4553,path[19].x);
	assertEquals(1498,path[19].y);
	assertEquals(4553,path[20].x);
	assertEquals(1498,path[20].y);
	assertEquals(4553.202,path[21].x);
	assertEquals(1486.858,path[21].y);
	assertEquals(4555.022,path[22].x);
	assertEquals(1484.044,path[22].y);
	}

	
	@Test
	public void test4410$667_1622_4430_1655(){
	 List<Vector2D> p = s.findPath(4410.667f,1622f,4430f,1655f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4452.254,path[0].x);
	assertEquals(1651.824,path[0].y);
	assertEquals(4452.766,path[1].x);
	assertEquals(1651.914,path[1].y);
	assertEquals(4453.098,path[2].x);
	assertEquals(1651.514,path[2].y);
	assertEquals(4453.098,path[3].x);
	assertEquals(1641.514,path[3].y);
	assertEquals(4452.91,path[4].x);
	assertEquals(1641.022,path[4].y);
	assertEquals(4452.391,path[5].x);
	assertEquals(1640.942,path[5].y);
	assertEquals(4447.391,path[6].x);
	assertEquals(1631.942,path[6].y);
	assertEquals(4447.058,path[7].x);
	assertEquals(1632.355,path[7].y);
	assertEquals(4440.058,path[8].x);
	assertEquals(1632.355,path[8].y);
	assertEquals(4423,path[9].x);
	assertEquals(1626,path[9].y);
	assertEquals(4416,path[10].x);
	assertEquals(1626,path[10].y);
	}

	@Test
	public void test4358$667_1546_4374_1570$667(){
	 List<Vector2D> p = s.findPath(4358.667f,1546f,4374f,1570.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4372,path[0].x);
	assertEquals(1564,path[0].y);
	assertEquals(4372,path[1].x);
	assertEquals(1564,path[1].y);
	assertEquals(4370,path[2].x);
	assertEquals(1562,path[2].y);
	assertEquals(4368,path[3].x);
	assertEquals(1559,path[3].y);
	assertEquals(4367,path[4].x);
	assertEquals(1558,path[4].y);
	assertEquals(4364,path[5].x);
	assertEquals(1554,path[5].y);
	assertEquals(4363,path[6].x);
	assertEquals(1552,path[6].y);
	assertEquals(4360,path[7].x);
	assertEquals(1549,path[7].y);
	assertEquals(4359,path[8].x);
	assertEquals(1548,path[8].y);
	}
	@Test
	public void test4421$333_1630_4430_1655(){
	 List<Vector2D> p = s.findPath(4421.333f,1630f,4430f,1655f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4433.254,path[0].x);
	assertEquals(1651.824,path[0].y);
	assertEquals(4433.058,path[1].x);
	assertEquals(1651.355,path[1].y);
	assertEquals(4433.058,path[2].x);
	assertEquals(1638.355,path[2].y);
	}
	
	@Test
	public void test3466_2110_3443_2110(){
	 List<Vector2D> p = s.findPath(3466f,2110f,3443f,2110f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3444,path[0].x);
	assertEquals(2108,path[0].y);
	assertEquals(3445,path[1].x);
	assertEquals(2108,path[1].y);
	assertEquals(3446,path[2].x);
	assertEquals(2108,path[2].y);
	assertEquals(3451,path[3].x);
	assertEquals(2108,path[3].y);
	assertEquals(3456,path[4].x);
	assertEquals(2108,path[4].y);
	assertEquals(3456,path[5].x);
	assertEquals(2108,path[5].y);
	assertEquals(3460,path[6].x);
	assertEquals(2108,path[6].y);
	assertEquals(3464,path[7].x);
	assertEquals(2108,path[7].y);
	assertEquals(3464,path[8].x);
	assertEquals(2108,path[8].y);
	}
	
	@Test
	public void test3466$667_2107$333_3444_2107$333(){
	 List<Vector2D> p = s.findPath(3466.667f,2107.333f,3444f,2107.333f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3444,path[0].x);
	assertEquals(2108,path[0].y);
	assertEquals(3445,path[1].x);
	assertEquals(2108,path[1].y);
	assertEquals(3446,path[2].x);
	assertEquals(2108,path[2].y);
	assertEquals(3451,path[3].x);
	assertEquals(2108,path[3].y);
	assertEquals(3456,path[4].x);
	assertEquals(2108,path[4].y);
	assertEquals(3456,path[5].x);
	assertEquals(2108,path[5].y);
	assertEquals(3460,path[6].x);
	assertEquals(2108,path[6].y);
	assertEquals(3464,path[7].x);
	assertEquals(2108,path[7].y);
	assertEquals(3464,path[8].x);
	assertEquals(2108,path[8].y);
	}
	
	@Test
	public void test3481_2111_3418_2108(){
	 List<Vector2D> p = s.findPath(3481f,2111f,3418f,2108f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3418,path[0].x);
	assertEquals(2108,path[0].y);
	assertEquals(3418,path[1].x);
	assertEquals(2108,path[1].y);
	assertEquals(3420,path[2].x);
	assertEquals(2108,path[2].y);
	assertEquals(3444,path[3].x);
	assertEquals(2108,path[3].y);
	assertEquals(3444,path[4].x);
	assertEquals(2108,path[4].y);
	assertEquals(3445,path[5].x);
	assertEquals(2108,path[5].y);
	assertEquals(3446,path[6].x);
	assertEquals(2108,path[6].y);
	assertEquals(3451,path[7].x);
	assertEquals(2108,path[7].y);
	assertEquals(3456,path[8].x);
	assertEquals(2108,path[8].y);
	assertEquals(3456,path[9].x);
	assertEquals(2108,path[9].y);
	assertEquals(3460,path[10].x);
	assertEquals(2108,path[10].y);
	assertEquals(3464,path[11].x);
	assertEquals(2108,path[11].y);
	assertEquals(3464,path[12].x);
	assertEquals(2108,path[12].y);
	assertEquals(3477,path[13].x);
	assertEquals(2109,path[13].y);
	}
	
	
	// debug it
	
	@Test
	public void test3526$667_2001$333_3626$667_1904(){
	 List<Vector2D> p = s.findPath(3526.667f,2001.333f,3626.667f,1904f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3617,path[0].x);
	assertEquals(1909,path[0].y);
	assertEquals(3608,path[1].x);
	assertEquals(1909,path[1].y);
	assertEquals(3572,path[2].x);
	assertEquals(1920,path[2].y);
	assertEquals(3550,path[3].x);
	assertEquals(1923,path[3].y);
	assertEquals(3550,path[4].x);
	assertEquals(1923,path[4].y);
	assertEquals(3549.974,path[5].x);
	assertEquals(1923.027,path[5].y);
	assertEquals(3545.974,path[6].x);
	assertEquals(1923.027,path[6].y);
	assertEquals(3545.974,path[7].x);
	assertEquals(1926.027,path[7].y);
	assertEquals(3545.974,path[8].x);
	assertEquals(1935.027,path[8].y);
	assertEquals(3545.974,path[9].x);
	assertEquals(1944,path[9].y);
	assertEquals(3545.974,path[10].x);
	assertEquals(1944,path[10].y);
	assertEquals(3545.974,path[11].x);
	assertEquals(1944.202,path[11].y);
	assertEquals(3545.974,path[12].x);
	assertEquals(1945.202,path[12].y);
	assertEquals(3546,path[13].x);
	assertEquals(1946,path[13].y);
	assertEquals(3546,path[14].x);
	assertEquals(1946,path[14].y);
	assertEquals(3543,path[15].x);
	assertEquals(1959,path[15].y);
	assertEquals(3549,path[16].x);
	assertEquals(1972,path[16].y);
	assertEquals(3536,path[17].x);
	assertEquals(1985,path[17].y);
	assertEquals(3536,path[18].x);
	assertEquals(1985,path[18].y);
	assertEquals(3526.037,path[19].x);
	assertEquals(1990.352,path[19].y);
	assertEquals(3525.998,path[20].x);
	assertEquals(1990.539,path[20].y);
	assertEquals(3523,path[21].x);
	assertEquals(1998,path[21].y);
	assertEquals(3523,path[22].x);
	assertEquals(1998,path[22].y);
	assertEquals(3525.125,path[23].x);
	assertEquals(2000.966,path[23].y);
	assertEquals(3525.146,path[24].x);
	assertEquals(2001.156,path[24].y);
	}
	
	@Test
	public void test3632_1904_3542_2006(){
	 List<Vector2D> p = s.findPath(3632f,1904f,3542f,2006f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3549,path[0].x);
	assertEquals(1998,path[0].y);
	assertEquals(3549,path[1].x);
	assertEquals(1998,path[1].y);
	assertEquals(3549,path[2].x);
	assertEquals(1972,path[2].y);
	assertEquals(3549,path[3].x);
	assertEquals(1972,path[3].y);
	assertEquals(3543,path[4].x);
	assertEquals(1959,path[4].y);
	assertEquals(3546,path[5].x);
	assertEquals(1946,path[5].y);
	assertEquals(3546,path[6].x);
	assertEquals(1946,path[6].y);
	assertEquals(3545.974,path[7].x);
	assertEquals(1945.202,path[7].y);
	assertEquals(3545.974,path[8].x);
	assertEquals(1944.202,path[8].y);
	assertEquals(3545.974,path[9].x);
	assertEquals(1944,path[9].y);
	assertEquals(3545.974,path[10].x);
	assertEquals(1944,path[10].y);
	assertEquals(3545.974,path[11].x);
	assertEquals(1935.027,path[11].y);
	assertEquals(3545.974,path[12].x);
	assertEquals(1926.027,path[12].y);
	assertEquals(3545.974,path[13].x);
	assertEquals(1923.027,path[13].y);
	assertEquals(3549.974,path[14].x);
	assertEquals(1923.027,path[14].y);
	assertEquals(3550,path[15].x);
	assertEquals(1923,path[15].y);
	assertEquals(3572,path[16].x);
	assertEquals(1920,path[16].y);
	assertEquals(3608,path[17].x);
	assertEquals(1909,path[17].y);
	assertEquals(3608,path[18].x);
	assertEquals(1909,path[18].y);
	assertEquals(3617,path[19].x);
	assertEquals(1909,path[19].y);
	}

	@Test
	public void test6409_1497_6682_1802(){
	 List<Vector2D> p = s.findPath(-6409f,1497f,-6682f,1802f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(-6688,path[0].x);
	assertEquals(1796,path[0].y);
	assertEquals(-6688,path[1].x);
	assertEquals(1792,path[1].y);
	assertEquals(-6688,path[2].x);
	assertEquals(1792,path[2].y);
	assertEquals(-6688,path[3].x);
	assertEquals(1778,path[3].y);
	assertEquals(-6656,path[4].x);
	assertEquals(1760,path[4].y);
	assertEquals(-6656,path[5].x);
	assertEquals(1760,path[5].y);
	assertEquals(-6641,path[6].x);
	assertEquals(1728,path[6].y);
	assertEquals(-6626,path[7].x);
	assertEquals(1699,path[7].y);
	assertEquals(-6624,path[8].x);
	assertEquals(1699,path[8].y);
	assertEquals(-6576,path[9].x);
	assertEquals(1632,path[9].y);
	assertEquals(-6528,path[10].x);
	assertEquals(1584,path[10].y);
	assertEquals(-6528,path[11].x);
	assertEquals(1584,path[11].y);
	assertEquals(-6488.112,path[12].x);
	assertEquals(1552.27,path[12].y);
	assertEquals(-6496,path[13].x);
	assertEquals(1536,path[13].y);
	assertEquals(-6496,path[14].x);
	assertEquals(1536,path[14].y);
	assertEquals(-6496,path[15].x);
	assertEquals(1534,path[15].y);
	assertEquals(-6464,path[16].x);
	assertEquals(1504,path[16].y);
	assertEquals(-6464,path[17].x);
	assertEquals(1504,path[17].y);
	assertEquals(-6442.609,path[18].x);
	assertEquals(1498.372,path[18].y);
	assertEquals(-6410.609,path[19].x);
	assertEquals(1498.372,path[19].y);
	}
	
	@Test
	public void test6661_1696_6580_1758(){
	 List<Vector2D> p = s.findPath(-6661f,1696f,-6580f,1758f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(-6592,path[0].x);
	assertEquals(1760,path[0].y);
	assertEquals(-6608,path[1].x);
	assertEquals(1728,path[1].y);
	assertEquals(-6624,path[2].x);
	assertEquals(1699,path[2].y);
	assertEquals(-6626,path[3].x);
	assertEquals(1699,path[3].y);
	}
	
	@Test
	public void test6769_1687_6442_1815(){
	 List<Vector2D> p = s.findPath(-6769f,1687f,-6442f,1815f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(-6528,path[0].x);
	assertEquals(1760,path[0].y);
	assertEquals(-6528,path[1].x);
	assertEquals(1760,path[1].y);
	assertEquals(-6560,path[2].x);
	assertEquals(1728,path[2].y);
	assertEquals(-6624,path[3].x);
	assertEquals(1699,path[3].y);
	assertEquals(-6626,path[4].x);
	assertEquals(1699,path[4].y);
	assertEquals(-6720,path[5].x);
	assertEquals(1686,path[5].y);
	assertEquals(-6768,path[6].x);
	assertEquals(1691,path[6].y);
	}
	
	@Test
	public void test6586_1797_6442_1815(){
	 List<Vector2D> p = s.findPath(-6586f,1797f,-6442f,1815f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(-6528,path[0].x);
	assertEquals(1808,path[0].y);
	assertEquals(-6528,path[1].x);
	assertEquals(1808,path[1].y);
	}
	
	
	
	
	@Test
	public void test4656_1834_4728_1873$333(){
	 List<Vector2D> p = s.findPath(4656f,1834f,4728f,1873.333f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4704,path[0].x);
	assertEquals(1879,path[0].y);
	assertEquals(4703,path[1].x);
	assertEquals(1879,path[1].y);
	assertEquals(4699.976,path[2].x);
	assertEquals(1879.435,path[2].y);
	assertEquals(4698.375,path[3].x);
	assertEquals(1879.796,path[3].y);
	assertEquals(4694.438,path[4].x);
	assertEquals(1880.508,path[4].y);
	assertEquals(4692.606,path[5].x);
	assertEquals(1880.767,path[5].y);
	assertEquals(4677.606,path[6].x);
	assertEquals(1880.767,path[6].y);
	assertEquals(4675.869,path[7].x);
	assertEquals(1879.596,path[7].y);
	assertEquals(4672.648,path[8].x);
	assertEquals(1877.189,path[8].y);
	assertEquals(4672,path[9].x);
	assertEquals(1876.646,path[9].y);
	assertEquals(4672,path[10].x);
	assertEquals(1876.646,path[10].y);
	assertEquals(4671.233,path[11].x);
	assertEquals(1876.004,path[11].y);
	assertEquals(4655.233,path[12].x);
	assertEquals(1863.004,path[12].y);
	assertEquals(4655.232,path[13].x);
	assertEquals(1863,path[13].y);
	assertEquals(4655.232,path[14].x);
	assertEquals(1862,path[14].y);
	assertEquals(4655.232,path[15].x);
	assertEquals(1862,path[15].y);
	assertEquals(4654.917,path[16].x);
	assertEquals(1860.425,path[16].y);
	assertEquals(4654.34,path[17].x);
	assertEquals(1856.685,path[17].y);
	assertEquals(4654.106,path[18].x);
	assertEquals(1854.698,path[18].y);
	assertEquals(4654.106,path[19].x);
	assertEquals(1839.698,path[19].y);
	assertEquals(4655.384,path[20].x);
	assertEquals(1837.94,path[20].y);
	assertEquals(4655.384,path[21].x);
	assertEquals(1836.94,path[21].y);
	}
	
	@Test
	public void test4535$333_1592$667_4506$667_1660$667(){
	 List<Vector2D> p = s.findPath(4535.333f,1592.667f,4506.667f,1660.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(4516,path[0].x);
	assertEquals(1638,path[0].y);
	assertEquals(4516,path[1].x);
	assertEquals(1635,path[1].y);
	assertEquals(4516,path[2].x);
	assertEquals(1632,path[2].y);
	assertEquals(4516,path[3].x);
	assertEquals(1632,path[3].y);
	assertEquals(4516,path[4].x);
	assertEquals(1630,path[4].y);
	assertEquals(4516,path[5].x);
	assertEquals(1625,path[5].y);
	assertEquals(4526,path[6].x);
	assertEquals(1615,path[6].y);
	assertEquals(4526,path[7].x);
	assertEquals(1613,path[7].y);
	assertEquals(4526,path[8].x);
	assertEquals(1612,path[8].y);
	assertEquals(4526,path[9].x);
	assertEquals(1609,path[9].y);
	assertEquals(4529,path[10].x);
	assertEquals(1608,path[10].y);
	assertEquals(4529,path[11].x);
	assertEquals(1604,path[11].y);
	}
	
	
	
	
	@Test
	public void test3549$333_1964$667_3545$333_1909$333(){
	 List<Vector2D> p = s.findPath(3549.333f,1964.667f,3545.333f,1909.333f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3546,path[0].x);
	assertEquals(1920,path[0].y);
	assertEquals(3545.974,path[1].x);
	assertEquals(1923.027,path[1].y);
	assertEquals(3545.974,path[2].x);
	assertEquals(1926.027,path[2].y);
	assertEquals(3545.974,path[3].x);
	assertEquals(1935.027,path[3].y);
	assertEquals(3545.974,path[4].x);
	assertEquals(1944,path[4].y);
	assertEquals(3545.974,path[5].x);
	assertEquals(1944,path[5].y);
	assertEquals(3545.974,path[6].x);
	assertEquals(1944.202,path[6].y);
	assertEquals(3545.974,path[7].x);
	assertEquals(1945.202,path[7].y);
	assertEquals(3546,path[8].x);
	assertEquals(1946,path[8].y);
	assertEquals(3546,path[9].x);
	assertEquals(1946,path[9].y);
	assertEquals(3543,path[10].x);
	assertEquals(1959,path[10].y);
	}
	
	
	
	// debug it first
	@Test
	public void test3610$667_2098_3670_2142(){
	 List<Vector2D> p = s.findPath(3610.667f,2098f,3670f,2142f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3672.026,path[0].x);
	assertEquals(2115.856,path[0].y);
	assertEquals(3672,path[1].x);
	assertEquals(2113,path[1].y);
	assertEquals(3648,path[2].x);
	assertEquals(2113,path[2].y);
	assertEquals(3648,path[3].x);
	assertEquals(2113,path[3].y);
	assertEquals(3639,path[4].x);
	assertEquals(2113,path[4].y);
	assertEquals(3630,path[5].x);
	assertEquals(2113,path[5].y);
	assertEquals(3630,path[6].x);
	assertEquals(2113,path[6].y);
	assertEquals(3629,path[7].x);
	assertEquals(2112,path[7].y);
	assertEquals(3629,path[8].x);
	assertEquals(2112,path[8].y);
	assertEquals(3628,path[9].x);
	assertEquals(2110,path[9].y);
	assertEquals(3620,path[10].x);
	assertEquals(2104,path[10].y);
	assertEquals(3620,path[11].x);
	assertEquals(2104,path[11].y);
	assertEquals(3618,path[12].x);
	assertEquals(2104,path[12].y);
	assertEquals(3611.591,path[13].x);
	assertEquals(2097.896,path[13].y);
	assertEquals(3611.422,path[14].x);
	assertEquals(2097.896,path[14].y);
	}
	
	@Test
	public void test3549$333_2082_3550$667_2145$333(){
	 List<Vector2D> p = s.findPath(3549.333f,2082f,3550.667f,2145.333f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3539,path[0].x);
	assertEquals(2138,path[0].y);
	assertEquals(3537.938,path[1].x);
	assertEquals(2134.491,path[1].y);
	assertEquals(3536.049,path[2].x);
	assertEquals(2133.042,path[2].y);
	assertEquals(3534.513,path[3].x);
	assertEquals(2131.209,path[3].y);
	assertEquals(3524.513,path[4].x);
	assertEquals(2131.209,path[4].y);
	assertEquals(3523.404,path[5].x);
	assertEquals(2129.049,path[5].y);
	assertEquals(3523.404,path[6].x);
	assertEquals(2116.049,path[6].y);
	assertEquals(3522.794,path[7].x);
	assertEquals(2113.618,path[7].y);
	assertEquals(3522.768,path[8].x);
	assertEquals(2112,path[8].y);
	assertEquals(3522.768,path[9].x);
	assertEquals(2112,path[9].y);
	assertEquals(3522.754,path[10].x);
	assertEquals(2111.112,path[10].y);
	assertEquals(3522.754,path[11].x);
	assertEquals(2108.112,path[11].y);
	assertEquals(3523.266,path[12].x);
	assertEquals(2105.739,path[12].y);
	assertEquals(3523.266,path[13].x);
	assertEquals(2103.739,path[13].y);
	assertEquals(3524.076,path[14].x);
	assertEquals(2102,path[14].y);
	assertEquals(3524.076,path[15].x);
	assertEquals(2102,path[15].y);
	assertEquals(3524.275,path[16].x);
	assertEquals(2101.571,path[16].y);
	assertEquals(3525.725,path[17].x);
	assertEquals(2099.682,path[17].y);
	assertEquals(3525.725,path[18].x);
	assertEquals(2088.682,path[18].y);
	assertEquals(3527.557,path[19].x);
	assertEquals(2087.146,path[19].y);
	assertEquals(3527.557,path[20].x);
	assertEquals(2085.146,path[20].y);
	assertEquals(3540.557,path[21].x);
	assertEquals(2085.146,path[21].y);
	assertEquals(3542,path[22].x);
	assertEquals(2084.406,path[22].y);
	assertEquals(3542,path[23].x);
	assertEquals(2084.406,path[23].y);
	assertEquals(3542.717,path[24].x);
	assertEquals(2084.037,path[24].y);
	assertEquals(3545.148,path[25].x);
	assertEquals(2083.427,path[25].y);
	assertEquals(3547.996,path[26].x);
	assertEquals(2083.455,path[26].y);
	}
	
	@Test
	public void test3548$667_2061$333_3550_2156(){
	 List<Vector2D> p = s.findPath(3548.667f,2061.333f,3550f,2156f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3536,path[0].x);
	assertEquals(2151,path[0].y);
	assertEquals(3536,path[1].x);
	assertEquals(2151,path[1].y);
	assertEquals(3526,path[2].x);
	assertEquals(2138,path[2].y);
	assertEquals(3526,path[3].x);
	assertEquals(2138,path[3].y);
	assertEquals(3524.513,path[4].x);
	assertEquals(2131.209,path[4].y);
	assertEquals(3523.404,path[5].x);
	assertEquals(2129.049,path[5].y);
	assertEquals(3523.404,path[6].x);
	assertEquals(2116.049,path[6].y);
	assertEquals(3522.794,path[7].x);
	assertEquals(2113.618,path[7].y);
	assertEquals(3522.768,path[8].x);
	assertEquals(2112,path[8].y);
	assertEquals(3522.768,path[9].x);
	assertEquals(2112,path[9].y);
	assertEquals(3522.754,path[10].x);
	assertEquals(2111.112,path[10].y);
	assertEquals(3522.754,path[11].x);
	assertEquals(2108.112,path[11].y);
	assertEquals(3523.266,path[12].x);
	assertEquals(2105.739,path[12].y);
	assertEquals(3523.266,path[13].x);
	assertEquals(2103.739,path[13].y);
	assertEquals(3524.076,path[14].x);
	assertEquals(2102,path[14].y);
	assertEquals(3524.076,path[15].x);
	assertEquals(2102,path[15].y);
	assertEquals(3524.275,path[16].x);
	assertEquals(2101.571,path[16].y);
	assertEquals(3525.725,path[17].x);
	assertEquals(2099.682,path[17].y);
	assertEquals(3525.725,path[18].x);
	assertEquals(2088.682,path[18].y);
	assertEquals(3527.557,path[19].x);
	assertEquals(2087.146,path[19].y);
	assertEquals(3527.557,path[20].x);
	assertEquals(2085.146,path[20].y);
	assertEquals(3529,path[21].x);
	assertEquals(2076,path[21].y);
	assertEquals(3529,path[22].x);
	assertEquals(2076,path[22].y);
	assertEquals(3531.502,path[23].x);
	assertEquals(2073.584,path[23].y);
	assertEquals(3531.482,path[24].x);
	assertEquals(2073.395,path[24].y);
	assertEquals(3531.267,path[25].x);
	assertEquals(2068.85,path[25].y);
	assertEquals(3539,path[26].x);
	assertEquals(2063,path[26].y);
	assertEquals(3542,path[27].x);
	assertEquals(2063,path[27].y);
	assertEquals(3542,path[28].x);
	assertEquals(2063,path[28].y);
	assertEquals(3543.276,path[29].x);
	assertEquals(2061.85,path[29].y);
	assertEquals(3543.802,path[30].x);
	assertEquals(2061.85,path[30].y);
	assertEquals(3547.37,path[31].x);
	assertEquals(2061.85,path[31].y);
	}
	
	@Test
	public void test3543$333_1900_3542$667_1988(){
	 List<Vector2D> p = s.findPath(3543.333f,1900f,3542.667f,1988f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3549,path[0].x);
	assertEquals(1972,path[0].y);
	assertEquals(3543,path[1].x);
	assertEquals(1959,path[1].y);
	assertEquals(3546,path[2].x);
	assertEquals(1946,path[2].y);
	assertEquals(3546,path[3].x);
	assertEquals(1946,path[3].y);
	assertEquals(3545.974,path[4].x);
	assertEquals(1945.202,path[4].y);
	assertEquals(3545.974,path[5].x);
	assertEquals(1944.202,path[5].y);
	assertEquals(3545.974,path[6].x);
	assertEquals(1944,path[6].y);
	assertEquals(3545.974,path[7].x);
	assertEquals(1944,path[7].y);
	assertEquals(3545.974,path[8].x);
	assertEquals(1935.027,path[8].y);
	assertEquals(3545.974,path[9].x);
	assertEquals(1926.027,path[9].y);
	assertEquals(3545.974,path[10].x);
	assertEquals(1923.027,path[10].y);
	assertEquals(3546,path[11].x);
	assertEquals(1920,path[11].y);
	assertEquals(3550,path[12].x);
	assertEquals(1906,path[12].y);
	}
	@Test
	public void test3476_1800$667_3576_1822(){
	 List<Vector2D> p = s.findPath(3476f,1800.667f,3576f,1822f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3570,path[0].x);
	assertEquals(1818,path[0].y);
	assertEquals(3532,path[1].x);
	assertEquals(1812,path[1].y);
	assertEquals(3494,path[2].x);
	assertEquals(1799,path[2].y);
	}
	
	// debug it first 
	@Test
	public void test3625$333_1840_3699$333_1852(){
	 List<Vector2D> p = s.findPath(3625.333f,1840f,3699.333f,1852f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3680,path[0].x);
	assertEquals(1842,path[0].y);
	assertEquals(3664,path[1].x);
	assertEquals(1842,path[1].y);
	assertEquals(3648,path[2].x);
	assertEquals(1832,path[2].y);
	assertEquals(3646,path[3].x);
	assertEquals(1839,path[3].y);
	assertEquals(3646,path[4].x);
	assertEquals(1839,path[4].y);
	}
	
	@Test
	public void test3628_1831$333_3481$333_1826$667(){
	 List<Vector2D> p = s.findPath(3628f,1831.333f,3481.333f,1826.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3494,path[0].x);
	assertEquals(1818,path[0].y);
	assertEquals(3532,path[1].x);
	assertEquals(1831,path[1].y);
	assertEquals(3570,path[2].x);
	assertEquals(1837,path[2].y);
	assertEquals(3570,path[3].x);
	assertEquals(1837,path[3].y);
	assertEquals(3589,path[4].x);
	assertEquals(1837,path[4].y);
	assertEquals(3608,path[5].x);
	assertEquals(1833,path[5].y);
	assertEquals(3608,path[6].x);
	assertEquals(1833,path[6].y);
	}
	
	@Test
	public void test3548$667_1849$333_3817$333_1864(){
	 List<Vector2D> p = s.findPath(3548.667f,1849.333f,3817.333f,1864f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3808,path[0].x);
	assertEquals(1863,path[0].y);
	assertEquals(3808,path[1].x);
	assertEquals(1863,path[1].y);
	assertEquals(3776,path[2].x);
	assertEquals(1865,path[2].y);
	assertEquals(3776,path[3].x);
	assertEquals(1865,path[3].y);
	assertEquals(3760,path[4].x);
	assertEquals(1865,path[4].y);
	assertEquals(3744,path[5].x);
	assertEquals(1858,path[5].y);
	assertEquals(3744,path[6].x);
	assertEquals(1858,path[6].y);
	assertEquals(3736.187,path[7].x);
	assertEquals(1857.418,path[7].y);
	assertEquals(3735.469,path[8].x);
	assertEquals(1857.38,path[8].y);
	assertEquals(3719.469,path[9].x);
	assertEquals(1857.38,path[9].y);
	assertEquals(3712,path[10].x);
	assertEquals(1857,path[10].y);
	assertEquals(3680,path[11].x);
	assertEquals(1858,path[11].y);
	assertEquals(3680,path[12].x);
	assertEquals(1858,path[12].y);
	assertEquals(3672.583,path[13].x);
	assertEquals(1853.103,path[13].y);
	assertEquals(3671.879,path[14].x);
	assertEquals(1852.954,path[14].y);
	assertEquals(3655.879,path[15].x);
	assertEquals(1852.954,path[15].y);
	assertEquals(3648,path[16].x);
	assertEquals(1850,path[16].y);
	assertEquals(3647,path[17].x);
	assertEquals(1856,path[17].y);
	assertEquals(3647,path[18].x);
	assertEquals(1856,path[18].y);
	assertEquals(3646.235,path[19].x);
	assertEquals(1857.507,path[19].y);
	assertEquals(3646,path[20].x);
	assertEquals(1857.612,path[20].y);
	assertEquals(3646,path[21].x);
	assertEquals(1857.612,path[21].y);
	assertEquals(3645.578,path[22].x);
	assertEquals(1857.8,path[22].y);
	assertEquals(3627,path[23].x);
	assertEquals(1858,path[23].y);
	assertEquals(3608,path[24].x);
	assertEquals(1852,path[24].y);
	assertEquals(3570,path[25].x);
	assertEquals(1856,path[25].y);
	}
	@Test
	public void test3609$333_2103$333_3657$333_2110(){
	 List<Vector2D> p = s.findPath(3609.333f,2103.333f,3657.333f,2110f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3648,path[0].x);
	assertEquals(2110,path[0].y);
	assertEquals(3639,path[1].x);
	assertEquals(2110,path[1].y);
	assertEquals(3629,path[2].x);
	assertEquals(2110,path[2].y);
	assertEquals(3628,path[3].x);
	assertEquals(2110,path[3].y);
	assertEquals(3620,path[4].x);
	assertEquals(2104,path[4].y);
	assertEquals(3620,path[5].x);
	assertEquals(2104,path[5].y);
	assertEquals(3618,path[6].x);
	assertEquals(2104,path[6].y);
	assertEquals(3611.591,path[7].x);
	assertEquals(2097.896,path[7].y);
	assertEquals(3611.422,path[8].x);
	assertEquals(2097.896,path[8].y);
	}
	
	
	
	@Test
	public void test3697$333_1854_3654$667_2044$667(){
	 List<Vector2D> p = s.findPath(3697.333f,1854f,3654.667f,2044.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(path[0].x,3672);
	assertEquals(path[0].y,2016);
	assertEquals(path[1].x,3672);
	assertEquals(path[1].y,2016);
	assertEquals(path[2].x,3672);
	assertEquals(path[2].y,1968);
	assertEquals(path[3].x,3672);
	assertEquals(path[3].y,1968);
	assertEquals(path[4].x,3672);
	assertEquals(path[4].y,1957);
	assertEquals(path[5].x,3680.146);
	assertEquals(path[5].y,1949.732);
	assertEquals(path[6].x,3688);
	assertEquals(path[6].y,1944);
	assertEquals(path[7].x,3688);
	assertEquals(path[7].y,1920);
	assertEquals(path[8].x,3695);
	assertEquals(path[8].y,1904);
	assertEquals(path[9].x,3696);
	assertEquals(path[9].y,1872);
	}
	
	
	@Test
	public void test3500$667_1850_3596$667_1842(){
	 List<Vector2D> p = s.findPath(3500.667f,1850f,3596.667f,1842f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3570,path[0].x);
	assertEquals(1837,path[0].y);
	assertEquals(3532,path[1].x);
	assertEquals(1850,path[1].y);
	}
	
	@Test
	public void test3668_2089$333_3762$667_1825$333(){
	 List<Vector2D> p = s.findPath(3668f,2089.333f,3762.667f,1825.333f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3760,path[0].x);
	assertEquals(1842,path[0].y);
	assertEquals(3760,path[1].x);
	assertEquals(1849,path[1].y);
	assertEquals(3744,path[2].x);
	assertEquals(1858,path[2].y);
	assertEquals(3744,path[3].x);
	assertEquals(1858,path[3].y);
	assertEquals(3736.187,path[4].x);
	assertEquals(1873.418,path[4].y);
	assertEquals(3736.484,path[5].x);
	assertEquals(1874,path[5].y);
	assertEquals(3736.484,path[6].x);
	assertEquals(1874,path[6].y);
	assertEquals(3740.64,path[7].x);
	assertEquals(1875.833,path[7].y);
	assertEquals(3741.297,path[8].x);
	assertEquals(1876.125,path[8].y);
	assertEquals(3741.372,path[9].x);
	assertEquals(1876.841,path[9].y);
	assertEquals(3741.372,path[10].x);
	assertEquals(1892.841,path[10].y);
	assertEquals(3740.79,path[11].x);
	assertEquals(1893.263,path[11].y);
	assertEquals(3742,path[12].x);
	assertEquals(1906,path[12].y);
	assertEquals(3742,path[13].x);
	assertEquals(1920,path[13].y);
	assertEquals(3720,path[14].x);
	assertEquals(1968,path[14].y);
	assertEquals(3696,path[15].x);
	assertEquals(1992,path[15].y);
	assertEquals(3672,path[16].x);
	assertEquals(2016,path[16].y);
	assertEquals(3672,path[17].x);
	assertEquals(2064,path[17].y);
	}
	
	@Test
	public void test3689$333_2150_3504$667_2136$667(){
	 List<Vector2D> p = s.findPath(3689.333f,2150f,3504.667f,2136.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(3513,path[0].x);
	assertEquals(2138,path[0].y);
	assertEquals(3513,path[1].x);
	assertEquals(2138,path[1].y);
	assertEquals(3526,path[2].x);
	assertEquals(2138,path[2].y);
	assertEquals(3526,path[3].x);
	assertEquals(2138,path[3].y);
	assertEquals(3534.513,path[4].x);
	assertEquals(2131.209,path[4].y);
	assertEquals(3536.049,path[5].x);
	assertEquals(2133.042,path[5].y);
	assertEquals(3537.938,path[6].x);
	assertEquals(2134.491,path[6].y);
	assertEquals(3539,path[7].x);
	assertEquals(2138,path[7].y);
	assertEquals(3552,path[8].x);
	assertEquals(2138,path[8].y);
	assertEquals(3552,path[9].x);
	assertEquals(2138,path[9].y);
	assertEquals(3557.985,path[10].x);
	assertEquals(2135.973,path[10].y);
	assertEquals(3560.416,path[11].x);
	assertEquals(2135.363,path[11].y);
	assertEquals(3562.576,path[12].x);
	assertEquals(2134.253,path[12].y);
	assertEquals(3565.576,path[13].x);
	assertEquals(2134.253,path[13].y);
	assertEquals(3567.409,path[14].x);
	assertEquals(2132.718,path[14].y);
	assertEquals(3568,path[15].x);
	assertEquals(2131.947,path[15].y);
	assertEquals(3568,path[16].x);
	assertEquals(2131.947,path[16].y);
	assertEquals(3578,path[17].x);
	assertEquals(2131.947,path[17].y);
	assertEquals(3578.858,path[18].x);
	assertEquals(2130.829,path[18].y);
	assertEquals(3579.867,path[19].x);
	assertEquals(2128.661,path[19].y);
	assertEquals(3581.867,path[20].x);
	assertEquals(2115.661,path[20].y);
	assertEquals(3582.379,path[21].x);
	assertEquals(2113.288,path[21].y);
	assertEquals(3582.359,path[22].x);
	assertEquals(2112,path[22].y);
	assertEquals(3582.359,path[23].x);
	assertEquals(2112,path[23].y);
	assertEquals(3582.339,path[24].x);
	assertEquals(2110.782,path[24].y);
	assertEquals(3592,path[25].x);
	assertEquals(2110,path[25].y);
	assertEquals(3616,path[26].x);
	assertEquals(2110,path[26].y);
	assertEquals(3620,path[27].x);
	assertEquals(2110,path[27].y);
	assertEquals(3624,path[28].x);
	assertEquals(2112,path[28].y);
	assertEquals(3624,path[29].x);
	assertEquals(2112,path[29].y);
	assertEquals(3629,path[30].x);
	assertEquals(2113,path[30].y);
	assertEquals(3630,path[31].x);
	assertEquals(2113,path[31].y);
	assertEquals(3630,path[32].x);
	assertEquals(2113,path[32].y);
	assertEquals(3639,path[33].x);
	assertEquals(2113,path[33].y);
	assertEquals(3648,path[34].x);
	assertEquals(2113,path[34].y);
	assertEquals(3648,path[35].x);
	assertEquals(2113,path[35].y);
	assertEquals(3672,path[36].x);
	assertEquals(2113,path[36].y);
	assertEquals(3672.026,path[37].x);
	assertEquals(2115.856,path[37].y);
	assertEquals(3688,path[38].x);
	assertEquals(2144,path[38].y);
	}
	
	@Test
	public void test3534_2057$333_3530_1988$667(){
	 List<Vector2D> p = s.findPath(3534f,2057.333f,3530f,1988.667f,1);
	Vector2D path [] = p.toArray(new Vector2D[p.size()]);
	assertEquals(path[0].x,3526.037);
	assertEquals(path[0].y,1990.352);
	assertEquals(path[1].x,3525.998);
	assertEquals(path[1].y,1990.539);
	assertEquals(path[2].x,3523);
	assertEquals(path[2].y,1998);
	assertEquals(path[3].x,3523);
	assertEquals(path[3].y,1998);
	assertEquals(path[4].x,3525.125);
	assertEquals(path[4].y,2000.966);
	assertEquals(path[5].x,3525.146);
	assertEquals(path[5].y,2001.156);
	assertEquals(path[6].x,3525.146);
	assertEquals(path[6].y,2014.156);
	assertEquals(path[7].x,3523);
	assertEquals(path[7].y,2020);
	assertEquals(path[8].x,3523);
	assertEquals(path[8].y,2024);
	assertEquals(path[9].x,3523);
	assertEquals(path[9].y,2024);
	assertEquals(path[10].x,3524.797);
	assertEquals(path[10].y,2024.035);
	assertEquals(path[11].x,3524.797);
	assertEquals(path[11].y,2025.665);
	assertEquals(path[12].x,3524.761);
	assertEquals(path[12].y,2031.544);
	assertEquals(path[13].x,3524.761);
	assertEquals(path[13].y,2033.193);
	assertEquals(path[14].x,3524.761);
	assertEquals(path[14].y,2046.193);
	assertEquals(path[15].x,3525.355);
	assertEquals(path[15].y,2048.422);
	assertEquals(path[16].x,3526);
	assertEquals(path[16].y,2050);
	assertEquals(path[17].x,3526);
	assertEquals(path[17].y,2050);
	assertEquals(path[18].x,3526.751);
	assertEquals(path[18].y,2053.117);
	assertEquals(path[19].x,3528.247);
	assertEquals(path[19].y,2055.66);
	assertEquals(path[20].x,3528.267);
	assertEquals(path[20].y,2055.85);
	}	 
	
}
	
	