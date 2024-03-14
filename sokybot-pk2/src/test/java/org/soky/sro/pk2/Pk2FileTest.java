package org.soky.sro.pk2;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * 
 * These unit tests run against real pk2 file , and the contents of this file
 * are the following : <br>
 * 
 * <table style= " border: 1px solid ;">
 * <thead style = "border: 1px solid ;">
 * <tr>
 * <th>File</th>
 * <th>Size</th>
 * </tr>
 * </thead> 
 * <tbody style= " border: 1px solid ;">
 * <tr>
 * <td style= " border: 1px solid ;">SV.T</td>
 * <td style= " border: 1px solid ;">1024 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\100x94.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\100x95.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\100x96.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\100x97.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\100x98.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\100x99.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap\101x100.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * minimap_d\egypt\rn_sd_egypt1_01_131x125.ddj</td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap_d\egypt\rn_sd_egypt1_01_131x128.ddj
 * </td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap_d\egypt\rn_sd_egypt1_01_131x130.ddj
 * </td>
 * <td style= " border: 1px solid ;">32916 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap_d\egypt\rn_sd_temple_2_128x127.ddj
 * </td>
 * <td style= " border: 1px solid ;">73876 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">minimap_d\egypt\rn_sd_temple_2_128x128.ddj
 * </td>
 * <td style= " border: 1px solid ;">73876 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">icon\skill\china\water_harmony_b.ddj</td>
 * <td style= " border: 1px solid ;">2196 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">icon\skill\china\water_heal_cycle_a.ddj
 * </td>
 * <td style= " border: 1px solid ;">4244 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\abusefilter.txt</td>
 * <td style= " border: 1px solid ;">4802678 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\characterdata.txt</td>
 * <td style= " border: 1px solid ;">9654 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\characterdata_10000.txt</td>
 * <td style= " border: 1px solid ;">76652 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\characterdata_10100.txt</td>
 * <td style= " border: 1px solid ;">76594 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\characterdata_10200.txt</td>
 * <td style= " border: 1px solid ;">58966 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\characterdata_10300.txt</td>
 * <td style= " border: 1px solid ;">71216 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">server_dep\silkroad\textdata\itemdata.txt
 * </td>
 * <td style= " border: 1px solid ;">21310 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\skilldata_10000enc.txt</td>
 * <td style= " border: 1px solid ;">4429622 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\skilldata_15000enc.txt</td>
 * <td style= " border: 1px solid ;">4245388 bytes</td>
 * </tr>
 * <tr>
 * <td style= " border: 1px solid ;">
 * server_dep\silkroad\textdata\skilldata_5000enc.txt</td>
 * <td style= " border: 1px solid ;">3788596 bytes</td>
 * </tr>
 * 
 * </tbody>
 * </table>
 * <br>
 * <br>
 * 
 * 
 * @author Amr
 *
 */
@Slf4j
public class Pk2FileTest {

	static private String filePath;
	static private IPk2Driver pk2File;

	@BeforeAll
	static void init() throws URISyntaxException {
		filePath = Pk2FileTest.class.getClassLoader().getResource("test.pk2").getPath();
		filePath = filePath.substring(1) ;
		System.out.println("File Path " + filePath) ; 
		pk2File = IPk2Driver.open(filePath) ; 
		
	}
	
	
	

	//@AfterAll
	static void destroy() throws IOException {
		pk2File.close();
	}

	//@Test
	public void testPk2FileFindWithLimitMethod() {

		List<JMXFile> files = pk2File.find("(?i)gateport.txt", 1);

		log.info("Files Size {} ", files.size());
		Assertions.assertEquals(1, files.size());

	}

	//@Test
	public void testQueryFilesMatchRegex() {
		// query each file with name (itemdata_[0-9]+.txt)
		String regex = "itemdata_[0-9]+.txt";
		List<JMXFile> files = pk2File.find(regex);

		Assertions.assertFalse(files.isEmpty());

		files.forEach((f) -> log.info("Item Data File : {} ", f));

	}

	//@Test
	void testQueryFileMatchRegex2() {
		String regex = "skilldata_(\\d+)(enc)?.txt$";

		List<JMXFile> files = pk2File.find(regex);
		Assertions.assertFalse(files.isEmpty());
		files.forEach((f) -> {
			log.info("Skill Data File : {} ", f);
		});

	}

	//@Test
	void testQueryEncryptedSkillDataFiles() {
		String regex = "skilldata_(\\d+)(enc).txt$";

		List<JMXFile> files = this.pk2File.find(regex);
		Assertions.assertTrue(!files.isEmpty());
		files.forEach((f) -> {
			log.info("Skill Data File : {} ", f);
		});

	}

	// this test case run correctly for LegionSRO_15_08_2019 Media.Pk2
	//@Test
	void testQueryCollectionAtParticularDir() {

		String requiredFiels = "\\icon64\\premium\\avatar\\(^([a-zA-Z0-9\\s_\\\\.\\-\\(\\):])+\\.(ddj)$)";

		List<JMXFile> files = this.pk2File.find(requiredFiels);

		Assertions.assertEquals(20, files.size());

	}

	//@Test
	public void testQueryCollectionAtParticularDir2() {

		String requiredFiels = "\\config\\(^([a-zA-Z0-9\\s_\\\\.\\-\\(\\):])+\\.(txt)$)";

		List<JMXFile> files = this.pk2File.find(requiredFiels);

		Assertions.assertEquals(6, files.size());

	}

//	@Test
	public void testQueryParticularFile() {
		String requiredFile = "\\config\\mp.txt";
		List<JMXFile> files = this.pk2File.find(requiredFile);

		Assertions.assertTrue(files.size() == 1);
	}

	//@Test
	public void testFindNotFound() {

		List<JMXFile> files = this.pk2File.find("NotFoundFile.NotFound", -1);

		log.info("Files Size {} ", files.size());
		Assertions.assertTrue(files.size() == 0);

	}

	//@Test
	public void testFindWithLimit0() {

		String initTest = "MyFile.txt";
		String regex = "(([a-zA-Z0_9]+)?.txt$)";
		Assertions.assertTrue(Pattern.matches(regex, initTest));
		List<JMXFile> files = this.pk2File.find(regex, 0);

		log.info("Files Size {} ", files.size());
		Assertions.assertTrue(files.size() == 0);

	}

//	@Test
	public void testFindWithLimit1() {

		String initTest = "MyFile.txt";
		String regex = "(([a-zA-Z0_9]+)?.txt$)";
		Assertions.assertTrue(Pattern.matches(regex, initTest));
		List<JMXFile> files = this.pk2File.find(regex, 1);

		log.info("Files Size {} ", files.size());
		Assertions.assertTrue(files.size() == 1);

	}

	//@Test
	public void testFindWithLimit2() {

		String initTest = "MyFile.txt";
		String regex = "(([a-zA-Z0_9]+)?.txt$)";
		Assertions.assertTrue(Pattern.matches(regex, initTest));
		List<JMXFile> files = this.pk2File.find(regex, 2);

		log.info("Files Size {} ", files.size());
		Assertions.assertTrue(files.size() == 2);

	}

//	@Test
	public void testPk2FileWithNullFilePath() {

	}

	//@Test
	public void testPk2FileWithNotValidFilePath() {

	}

	//@Test
	public void testPk2FileInterfaceWithNotPk2File() {

	}

}
