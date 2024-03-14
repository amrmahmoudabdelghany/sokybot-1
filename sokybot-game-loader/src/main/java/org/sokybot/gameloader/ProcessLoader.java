package org.sokybot.gameloader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.IKernel32;
import org.sokybot.gameloader.injector.IInjector;
import org.sokybot.gameloader.injector.Injector;
import org.sokybot.gameloader.injector.shellcode.IShellCode;
import org.sokybot.gameloader.injector.shellcode.ShellCode;

import com.kichik.pecoff4j.io.PEParser;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinBase.STARTUPINFO;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;



public class ProcessLoader implements IProcessLoader {

	
	
	
	@Override
	public int loadProcessImage(String imagePath,String command ,  String dllPath , String shellCodePath) {
		
		PROCESS_INFORMATION pi = openProcess("\"" + imagePath + "\""  + command) ; 
		int imageEntryPoint;
		try {
			imageEntryPoint = getImageEntryPoint(imagePath);
		} catch (IOException e) {
			throw new RuntimeException("Can not get image entry point ") ; 
			//e.printStackTrace();
		} 
		System.out.println("Image Entry Point : " + Integer.toHexString(imageEntryPoint )) ; 
		byte []OEPBytes = ProcessIO.read(pi.hProcess, new Pointer(imageEntryPoint), 6) ; 
		
        // here we must get OEP 
		// and read OEP bytes
		
		// prepare shellcode
		IShellCode shellCode = ShellCode.createShellCode(shellCodePath) ; 
		int address = shellCode.putString(dllPath) ;
		shellCode.assignPtr("DllPath", address); 
		address = shellCode.putString("Initialize") ; 
		shellCode.assignPtr("StartFunName", address);
		address = shellCode.putArr(OEPBytes) ; 
		shellCode.assignPtr("origBytes", address);
		//ByteBuffer buffer = ByteBuffer.allocate(4) ;
		//buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(imageEntryPoint) ; 
		//address = shellCode.putArr(buffer.array()) ; 
		 
		shellCode.assignValue("restoreAddr", imageEntryPoint);
		shellCode.assignPtr("allocAddr", 0x00);
	     
		//System.out.println("Relative Address for RestoreAddr : " + address) ; 
		//address = shellCode.putInt(0x00) ; 
		
		
		IInjector injector = new Injector()   ;
	
		Pointer allocAddress = injector.inject(pi.hProcess, shellCode) ; 
	    // System.out.println("AllocAddress : " + Long.toHexString(Pointer.nativeValue(allocAddress))) ;
	     //ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt() ; 
	     
		ByteBuffer patch = ByteBuffer.allocate(6) ; 
		patch.order(ByteOrder.LITTLE_ENDIAN)
			 .put((byte)0xE8)
			 .putInt((shellCode.getCodeEntryPoint() + (int)Pointer.nativeValue(allocAddress)) -
					 							(imageEntryPoint + 5) )
			 .put((byte)0x90)
			 .flip(); 
		ProcessIO.write(pi.hProcess, new Pointer(imageEntryPoint), patch.array()) ; 
		 // write patch 
		
		IKernel32.INSTANCE.ResumeThread(pi.hProcess) ; 
	    IKernel32.INSTANCE.ResumeThread(pi.hThread) ; 
		
	   return pi.dwProcessId.shortValue() ;
	   
		//return Pointer.nativeValue(pi.hProcess.getPointer());
	}
	
	
	private int getImageEntryPoint(String imagePath) throws IOException { 
		com.kichik.pecoff4j.PE pe = PEParser.parse(new File(imagePath)) ; 
		   return (int)  (pe.getOptionalHeader().getAddressOfEntryPoint() + 
				  pe.getOptionalHeader().getImageBase()) ;
	}
	private PROCESS_INFORMATION openProcess(String imagePath) { 
		   PROCESS_INFORMATION pi = new PROCESS_INFORMATION() ; 
		    
		   boolean res = IKernel32.
			INSTANCE.
			CreateProcessA(null,//lpApplicationName			   String
					imagePath.getBytes(),//lpCommandLine					   String
					null,//lpProcessAttributes				   WinBase.SecurityAttributes
					null,//lpThreadAttributes				   WinBase.SecurityAttributes
					false,//bInheritHandles                    boolean
					 new DWORD(WinBase.CREATE_SUSPENDED),//dwCreationFlags WinBase.DWORD
					null,//lpEnvironment                       Pointer
					null,//lpCurrentDirectory  				   String
					new STARTUPINFO(), 
					pi) ;
		   
		   
		   if(pi.hProcess == WinBase.INVALID_HANDLE_VALUE) { 
			   System.err.println("Invalid process handle") ; 
		   }
		   if(!res) { 
			  
			   System.err.println("Can not create process ' " + imagePath + " ' ") ;
		   }
		  return pi ; 
	}
	
	
  public static  IProcessLoader createInstance() { 
	  return new ProcessLoader() ; 
  }
}
