package org.sokybot.pk2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Stack;
import static org.junit.jupiter.api.Assertions.*;

public class Pk2FileTest {

    private Pk2File pk2File;

    @BeforeEach
    public void setUp() {
         // Pk2File constructor expects a file, but we only want to test utility methods
         // We can use a mocked or dummy path as long as we don't open it
         pk2File = new Pk2File("dummy_path.pk2");
    }

    @Test
    public void testResolvePattern_SimpleWildcard() {
        // *.nvm -> .*\.nvm
        String result = pk2File.resolvePattern("*.nvm");
        assertEquals(".*\\.nvm", result);
    }
    
    @Test
    public void testResolvePattern_WildcardInMiddle() {
        // file*.txt -> file.*\.txt
        String result = pk2File.resolvePattern("file*.txt");
        assertEquals("file.*\\.txt", result);
    }
    
    @Test
    public void testResolvePattern_NoWildcard() {
        // file.txt -> file.txt (no change)
        String input = "file.txt";
        String result = pk2File.resolvePattern(input);
        assertEquals(input, result);
    }
    
    @Test
    public void testResolvePattern_ExistingRegex() {
        // .*\.txt -> .*\.txt (no change because it detects regex chars)
        String input = ".*\\.txt";
        // It has special chars * and \, so it should be detected as regex
        String result = pk2File.resolvePattern(input);
        assertEquals(input, result);
    }
    
    @Test
    public void testGetPathStack_NormalPath() {
        // dir/file -> [file, dir] (Stack pushes: file then dir. Pop order: dir then file)
        Stack<String> stack = pk2File.getPathStack("dir/file");
        assertEquals(2, stack.size());
        assertEquals("dir", stack.pop());
        assertEquals("file", stack.pop());
    }

    @Test
    public void testGetPathStack_Empty() {
        Stack<String> stack = pk2File.getPathStack("");
        assertTrue(stack.isEmpty(), "Empty path should result in empty stack");
    }

    @Test
    public void testGetPathStack_RootPath() {
        // / -> [] (empty path components)
        Stack<String> stack = pk2File.getPathStack("/");
        assertTrue(stack.isEmpty(), "Root path should result in empty stack if handled as separator only");
    }

    @Test
    public void testResolvePattern_Null() {
        assertEquals("", pk2File.resolvePattern(null));
    }

    @Test
    public void testResolvePattern_RegexLikeWildcard() {
        // [a-z]*.txt -> Should be treated as regex because of []
        String input = "[a-z]*.txt"; 
        assertEquals(input, pk2File.resolvePattern(input));
    }
    
    @Test
    public void testGetPathStack_MockRegex() {
        // This tests internal logic that splits the path
        // .*\.t -> [.*\.t]
        // Since getPathStack uses / separator
        Stack<String> stack = pk2File.getPathStack(".*\\.t");
        assertEquals(1, stack.size());
        assertEquals(".*\\.t", stack.pop());
    }
    
    @Test
    public void testGetPathStack_EscapedDots() {
        // file\.name -> [file\.name]
        Stack<String> stack = pk2File.getPathStack("file\\.name");
        assertEquals(1, stack.size());
        assertEquals("file\\.name", stack.pop());
    }
}
