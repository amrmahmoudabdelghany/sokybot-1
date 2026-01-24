package org.sokybot.gameloader.injector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sokybot.gameloader.expressions.IExpression;
import org.sokybot.gameloader.injector.resolvers.IResolver;




public class ShellParser implements IShellParser{

 private final Map<String , ICounter> counters ; 
 private final List<Integer> relativeAddr ; 
 private final IResolver resolver ; 
 private final Map<String , List<Integer>> params ;
 
 public ShellParser(Map<String, ICounter> counters ,List<Integer> relativeAddr ,Map<String , List<Integer>> params ,  IResolver resolver) { 
	 this.counters = counters ; 
	 this.relativeAddr = relativeAddr ; 
	 this.resolver = resolver ; 
	 this.params = params ; 
 }
 
	
	
	@Override
	public ByteBuffer parse(File shellFile) {
		// this buffer will contain machine code to be injected
		ByteBuffer buffer = ByteBuffer.allocate(1024 ) ; 
		 buffer = buffer.order(ByteOrder.LITTLE_ENDIAN) ; 
		 
		try {
			FileInputStream finput = new FileInputStream(shellFile) ;
			byte data = -1 ; 
			int index = -1 ; 
			 
			 
			//System.out.println("------- Expresions ------") ; 
 			   while((data = (byte)finput.read()) != -1) { 
 				     if(!isValidChar(data)) continue ; 
 				        
 				       		
 				    if(data == '$') { 
 				    	String exp = readExp(finput) ; 
 				        //System.out.println(charEXP) ; 
 				    	// here we must handle expressions 
 				    	
 				    	 IExpression result =  this.resolver.resolve(exp) ; 
 				    	 
 				    	 switch(result.command()) { 
 				    	   
 				    	 case "I" : 
 				    		 this.counters.get(result.value()).encounter(index + 1);
 				    		 continue ;  
 				    	 case "R" : 
 				    		
 				    		 String val = result.value() ;
 				    		 if(val.length() != 8) { 
 				    			 throw new RuntimeException("expect 4-bytes for expression '" + exp + "'" ) ;
 				    		 }
 				    		 try { 
 				    			int decode = Integer.parseInt(val, 16) ;  
 				    			buffer.putInt(decode) ; 
 				    			 this.relativeAddr.add(index + 1) ; 
 				    			index += 4 ; 
 		     				 }catch(NumberFormatException ex) { 
 				    				 throw new RuntimeException("Invalid value within expression  " + "'" +exp + "'") ; 
 				    		 }
 				    		 break ; 
 				    	 case "P" :
 				    		 
 				    		 
 				    		 buffer.putInt(0x00) ; 
 				    		 List<Integer> paramOffsets = this.params.get(result.value()) ; 
 				    		 if(paramOffsets == null ) { 
 				    			 paramOffsets = new ArrayList<>() ; 
 				    			 this.params.put(result.value(), paramOffsets ) ; 
 				    		 }
 				    		 paramOffsets.add(index + 1) ; 
 				    		//this.counters.get(result.value()).encounter(index + 1);
 				    		 index+=4 ; 
 				    		 break  ; 
 				    		 
 				    		 default : 
 				    			 
 				    	 
 				    	 }
 				    	
 				    	 //System.out.println("EXP : " + charEXP + " ,Evaluation :  " + result  ) ; 
 				    }else { 
 				        String hexByte = "" ; 
 				     	hexByte += (char) data ; 
 				     	if((data = (byte)finput.read())!= -1 && isValidChar(data)) {
 				        hexByte += (char) data; 
 				        index ++ ;
 				     	}else {
 				     	long currentPos = finput.getChannel().position() ; 	
 				     	throw new RuntimeException("Invalid byte at posion : " + currentPos) ; 
 				     	}
 				     	buffer.put((byte)Short.parseShort(hexByte, 16)) ; 
 				     	
 				     	//System.out.print("0x"+hexByte + ((index % 4 == 0)?"\n" : " ")) ; 
 				     	
 				    }
 				    
				   
			   }
 			   
 			
 			   finput.close();
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	     
		
		
		
		
		return buffer;
	}
	
	// $PX{asdasd}
	private String readExp(FileInputStream finput) throws IOException { 
		 byte data  = -1 ; 
		 String exp = "" ;
		   long currentPos = finput.getChannel().position() ; 
		   while((data = (byte) finput.read()) != -1 && (data != '}')) { 
			 exp +=(char) data ; 
		   }
		   
		   if(data == -1) { 
			   throw new RuntimeException("expect expression after $") ; 
		   }
		   if(data != '}') { 
			   throw new RuntimeException("expression must enclosed within {} " + currentPos ) ; 
		   }
		   exp += (char) data ; 
		return exp ; 
	}
	
	
	private boolean isValidChar(byte data) { 
		 
		  return (data == '$' || 
				data == '{' ||
				data == '}' ||
				(data >= '0' && data <= '9') || 
				(data >= 'A' && data <= 'Z' )|| 
				(data >= 'a' && data <= 'z') ); 

	}
	
}
