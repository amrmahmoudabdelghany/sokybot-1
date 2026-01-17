
package org.sokybot.utils ; 
import java.util.Arrays;

/**
 *
 * @author AMROO
 */
public class Bytes {
     
    public static  Byte[] toObject(byte[] bytes) {
        Byte[] res  = new Byte[bytes.length] ; 
        int i = 0 ; 
        for(byte b : bytes) {
            res[i++] = b;
        }
        return res ; 
    }
    public static byte[] toPrimative(Byte[] bytes) {
        byte [] res = new byte[bytes.length] ; 
        int i =0  ; 
        for(Byte b : bytes) {
            res[i++] = b ; 
        }
        return res ; 
    }
    public static  byte[] _break(Byte[] src  , int length) {
        byte [] res = new byte[length]; 
         System.arraycopy(toPrimative(src), 0, res, 0, length);
         
        return res ; 
    }
    
    
    public static void main(String args[]) {
        Byte [] arrs = {5 , 4 , 3 , 2 , 1 , 0 } ; 
        _break(arrs, 2);
       System.out.println(Arrays.toString(arrs));
    }
}
