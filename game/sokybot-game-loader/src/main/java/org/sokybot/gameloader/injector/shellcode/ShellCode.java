package org.sokybot.gameloader.injector.shellcode;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sokybot.gameloader.injector.EntryPointCounter;
import org.sokybot.gameloader.injector.ICounter;
import org.sokybot.gameloader.injector.IEntryPointCounter;
import org.sokybot.gameloader.injector.IShellParser;
import org.sokybot.gameloader.injector.ShellParser;
import org.sokybot.gameloader.injector.resolvers.IResolver;
import org.sokybot.gameloader.injector.resolvers.ResolverFactory;



public class ShellCode  implements IShellCode{

	
	private final ByteBuffer buffer  ; 
	private final IEntryPointCounter entryPoint ; 
	private final Map<String , List<Integer>> params  ; 
	private final List<Integer> relativeAddr ; 
	
	private ShellCode(ByteBuffer buffer , IEntryPointCounter entryPoint,
		            	Map<String , List<Integer>> params ,
		            	  List<Integer> relativeAddr) { 
		
		this.buffer = buffer ;
		this.entryPoint = entryPoint ;  
		this.params = params ; 
		this.relativeAddr  = relativeAddr ; 
	}


	@Override
	public void assignPtr(String varName, int val) {
		if(!this.params.containsKey(varName)) { 
			throw new RuntimeException("Variable " + varName + " is undefined") ;
		}
		this.buffer.order(ByteOrder.LITTLE_ENDIAN) ;
		
		List<Integer> offsets = this.params.get(varName) ; 
		for(Integer i : offsets) { 
			this.buffer.putInt(i, val) ; 
			this.relativeAddr.add(i) ; 
		}
 		
	}
	
	@Override
	public void assignValue(String varName, int val) {
		if(!this.params.containsKey(varName)) { 
			throw new RuntimeException("Variable " + varName + " is undefined") ;
		}
		List<Integer> offsets = this.params.get(varName) ; 
		for(Integer i : offsets) { 
			this.buffer.putInt(i, val) ; 
			
		}
	}
	@Override
	public byte[] bytes() {
		
		int pos = this.buffer.position() ; 
		byte [] arr = new byte[pos] ; 
		this.buffer.position(0) ; 
		this.buffer.get(arr) ;
		this.buffer.position(pos) ; 
		return arr ; 
	}
	
	@Override
	public int putInt(int val) {
		int pos = this.buffer.position() ; 
		this.buffer.order(ByteOrder.LITTLE_ENDIAN) ; 
		this.buffer.putInt(val);  
		return pos;
	} 
	@Override
	public int putString(String str) {
       
		int pos = this.buffer.position() ; 
		this.buffer.put(str.getBytes()) ; 
		this.buffer.put((byte)0x00) ;
		return pos;
	}
	@Override
	public int putArr(byte[] arr) {
		int pos = this.buffer.position() ; 
		this.buffer.put(arr) ; 
		return pos;
	}
	
	@Override
	public void rebase(int newBase) {
		this.buffer.order(ByteOrder.LITTLE_ENDIAN) ;
		for(Integer i : this.relativeAddr) { 
			int rAddr = this.buffer.getInt(i) ; 
			int vAddr = newBase + rAddr ;
			this.buffer.putInt(i, vAddr) ; 
		}
	}
	
	
	@Override
	public int getCodeEntryPoint() {
			
		return this.entryPoint.getRelativeEntryPoint();
	}
	
	@Override
	public int size() {
		return this.buffer.position() ; 
		
	}
	
	@Override
	public void dump() {
		System.out.println("------- Buffer info ------") ; 
		System.out.println("Buffer Position : " + this.buffer.position()) ; 
		System.out.println("Buffer Limit : " + this.buffer.limit()) ; 
		System.out.println("Buffer Capcity : " + this.buffer.capacity()) ;
		
		System.out.println("------- Variables ------") ; 
		Set<String> names = this.params.keySet() ; 
		for(String name : names) { 
		 List<Integer> offsets = this.params.get(name) ;
		  for(Integer i : offsets) { 
			  System.out.print("Variable : " + name +  " , offsets : " ) ; 
			  System.out.print(i + ",value : " + this.buffer.getInt(i));
			  
 		  }
		  System.out.println(); 
			
		}
		System.out.println("-------Relative Address ----") ; 
		for(Integer i : this.relativeAddr) { 
			System.out.println("Address : " + i + " , value : " + this.buffer.getInt(i)) ; 
		}
		byte [] arr = new byte[this.buffer.position() ] ; 
		this.buffer.flip() ;  
		this.buffer.get(arr) ; 
		 
	 String res =	bytesToHex(arr) ; 
	   for(int i = 0 ; i < res.length() ; i+=2) { 
		   System.out.print("0x" + res.charAt(i) + res.charAt(i+ 1) + (( i%50 == 0)? '\n' :' ' ));
	   }
       byte arr2[] = new byte[37] ; 
       this.buffer.position(582) ; 
       this.buffer.get(arr2) ; 
       
       System.out.println("------ DLL Path is -----") ; 
       System.out.println(new String(arr2)) ; 
	}
	public static IShellCode createShellCode(String shellPath) { 
		
		  File file = new File(shellPath) ; 
		  Map<String,ICounter> counters = new HashMap<>() ; 
		  IEntryPointCounter entryPointCounter = new EntryPointCounter() ; 
		  counters.put("EP", entryPointCounter) ; 
		  
		  Map<String , List<Integer>> params  = new HashMap<>() ; 
		  List<Integer> relativeAddr = new ArrayList<>() ; 
		  IResolver resolvers = ResolverFactory.createResolversChain() ; 
		  IShellParser shellParser = new ShellParser(counters, relativeAddr, params, resolvers) ; 
		  ByteBuffer buffer =  shellParser.parse(file) ; 

			return new ShellCode(buffer, entryPointCounter, params , relativeAddr) ; 
		}
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	
}
