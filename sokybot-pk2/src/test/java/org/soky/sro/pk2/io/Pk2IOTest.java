package org.soky.sro.pk2.io;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class Pk2IOTest {


    static private String filePath ;
    static private IPk2Driver pk2File ;


    //@BeforeAll
    static void init() {
        filePath =  "E:\\Amroo\\Silkroad Games\\LegionSRO_15_08_2019\\Media.pk2";
        pk2File = IPk2Driver.open(filePath);


    }

    //@AfterAll
    static  void destroy() throws IOException {
        pk2File.close();
    }


    
    

    //@Test
    void testInputStream() {

    	
        pk2File.find("itemdata.txt").forEach((itemDataFile)->{

            log.debug("Analyzing itemdata.txt file");

                Reader reader = new InputStreamReader( itemDataFile.getInputStream() , StandardCharsets.UTF_16) ;

                BufferedReader br = new BufferedReader(reader) ;
               // Scanner      scanner = new Scanner( Pk2IO.getInputStream(itemDataFile) , StandardCharsets.UTF_16LE);

                String regex = "(?i)ItemData_(\\d+)\\.txt$" ; //"(?i)itemdata_(\\d+).txt")
                br.lines().forEach((l)->{
                log.info(l) ;
                boolean match =  l.matches(regex) ;
                 log.info("line {} {} matches {}" , l ,(match) ? "" : "not" ,  regex);
                  assertTrue(match);
                });


                // assertTrue(scanner.hasNext());
               // while(scanner.hasNextLine()) {
                 //   String line = scanner.nextLine() ;
                  //  log.info(line) ;

                   // assertTrue(line.equals("\uFEFFItemData_5000.txt"));

               // }


        });

    }

    //@Test
    void testInputStreamForParticularFile() {
        String fileName  = "skilldataenc.txt" ;

         try(InputStream input = pk2File.findFirst(fileName).map(JMXFile::getInputStream).orElseThrow()) {

            long actual =  IOUtils.consume(input) ;
            log.info("Consumed Bytes {}" , actual);
            assertTrue(actual > 0);
         }catch (IOException ex) {
             log.error(ex.getMessage());
         }


    }

    //@Test
    void testInputStreamForEmptyTextFile() {
        String fileName = "eventnpcdata.txt" ;

       JMXFile jmxFile  =  pk2File.find(fileName , 1).stream().findFirst().orElseThrow() ;

         InputStreamReader reader = new InputStreamReader( jmxFile.getInputStream());
       long lines =   new BufferedReader(reader).lines().count()  ;
       assertEquals(0 , lines);
       

    }
    
    //@Test
    void testInputStreamForAnyText() {

        pk2File.find( "(([a-zA-Z0_9]+)?.txt$)").forEach((jmxFile)->{
            try {
                //BufferedReader reader = new BufferedReader(new InputStreamReader(Pk2IO.getInputStream(jmxFile))) ;

                long actualSize = IOUtils.consume(jmxFile.getInputStream()) ;

                log.info("Consuming {} byte(s) from file {} with size {} " ,actualSize ,  jmxFile.getName() , jmxFile.getSize());
                assertEquals( jmxFile.getSize() , actualSize );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }


    //@Test
    void testInputStreamForParticularEncText() throws IOException {
        Path p = Paths.get("src/test/resources/encoutput.txt") ;

        if(Files.notExists(p)) {
                Files.createFile(p) ;
        }else {
            Files.delete(p);
        }
        FileOutputStream out = new FileOutputStream(p.toFile()) ;

        String fileName = "skilldata_20000enc.txt" ;
       try(InputStream input =  pk2File.findFirst(fileName).map(JMXFile::getInputStream).orElseThrow()) {
          // BufferedReader reader = new BufferedReader(new InputStreamReader(input , StandardCharsets.UTF_16LE)) ) {
                input.transferTo(out) ;

       }catch(IOException ex) {
            log.error(ex.getMessage()) ;
       }

    }

    // this test case incomplete


   // @Test
    void testInputStreamForAnyDDjFile() {

        pk2File.find( "(([a-zA-Z0_9]+)?.ddj$)").forEach((jmxFile)->{
            try(InputStream input = jmxFile.getInputStream()) {

                long actualSize = IOUtils.consume(input) ;

                log.info("Consuming {} byte(s) from file {} with size {} " ,actualSize ,  jmxFile.getName() , jmxFile.getSize());
                assertEquals( jmxFile.getSize() , actualSize );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

}