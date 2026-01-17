
package org.sokybot.utils ; 


import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Amr
 */
public class Converter {

   public static String bytesToHex(byte[] bytes) {
       char[] hexArray = "0123456789ABCDEF".toCharArray();
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
}
   public static String splitHex(String val){
       
       char str[] = val.toCharArray();
       String var = "";
       for(int i = 0 ; i <str.length ; i +=1){
           if(i%2 == 0 )
           var += str[i] ;
           else 
           var += str[i]+" ";
       }
       return var ;
   }
    public static void reverse8Bytes(byte s[])
     {
         byte byteTmp[] = new byte [8];
         System.arraycopy(s,0, byteTmp, 0, 8);
         s[0] = byteTmp[3];
         s[1] = byteTmp[2];
         s[2] = byteTmp[1];
         s[3] = byteTmp[0];
         
         s[4] = byteTmp[7];
         s[5] = byteTmp[6];
         s[6] = byteTmp[5];
         s[7] = byteTmp[4];
         
     
     }
 
       public static void inversBytes(byte[]s )
       {
           byte ByteTmp[]  = new byte[4];
           System.arraycopy(s, 0, ByteTmp, 0, 4);
           s[3] = ByteTmp[0];
           s[2] = ByteTmp[1];
           s[1] = ByteTmp[2];
           s[0] = ByteTmp[3];
           
           
           
       }
  
  public static long toLong(byte s[])
   {
       
       int val =0;
       val = val <<1;
       
       for(int i = 0;i<s.length;i++)
       {
         
           val =val<<8;
         val = val ^s[i];
         
       }
       
       return  Integer.toUnsignedLong(val);
       
       
       
   }
 
public static byte [] toBytes2(long s)
{
   long  LongVal = s;
    byte [] val = new byte [4];
    
    for(int i = 0;i<val.length;i++)
    {
        val[i] =(byte) (LongVal>>(24-(i*8)));//0,8,16,24
        
    }
    
    
    return val;
    
}
 public static byte [] toBytes(long s)
{
   long  LongVal = s;
    byte [] val = new byte [4];
    
    
        val[0] =(byte) (LongVal>>24);
        val[1] = (byte)(LongVal>>16);
        val[2] = (byte)(LongVal>>8);
        val[3] = (byte)(LongVal);
    
    
    
    return val;
    
}
 public static long toUnsignedLong(byte []byteval)
    {
        long val = 0;
        for(int i = 0;i<byteval.length;i++)
        {
            
            val = val  ^(byteval[i]&0xff);
            if(i!=(byteval.length-1))
                val<<=8;
                    
            
        }
        return val;
    }
 public static void Bits16ToBytes(int in, byte[] b, int offset)
        {
                b[offset ] = (byte) in;
                b[offset +1] = (byte) (in >> 8);

        }
 
   public static long getUInt32(byte [] val) throws EOFException, IOException{
         
     byte[] bytes = val;
    long value = bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24);
    return value & 0x00000000FFFFFFFFL;

       
   }
   
    public static void main(String arg[]){
        byte s[] = {15 , -10 , 20 , 35 , 55 } ;
        System.out.println(bytesToHex(s));
        
        List<String> list = new ArrayList<>() ; 
        list.add("amr") ; 
        list.add("mahmoud") ; 
        list.add("ahmed") ; 
        String [] arrList = list.toArray(new String[0]);
         for(int i = 0 ; i < arrList.length ; i++) {
        	 System.out.println(arrList[i] ) ; 
         }
    }
    
}
