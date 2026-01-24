package org.sokybot.gameloader.injector;

import org.sokybot.gameloader.injector.shellcode.IShellCode;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;

import org.sokybot.gameloader.ProcessIO;



public class Injector implements IInjector {

	
	
	@Override
	public Pointer inject(HANDLE processHandle, IShellCode shellCode) {
		 
		   SIZE_T size = new SIZE_T(shellCode.size()) ; 
		  Pointer allocAddress = Kernel32.INSTANCE.VirtualAllocEx(processHandle, Pointer.NULL,size, 
				  0x00001000 | 0x00002000, 0x40) ; 
		
		  if(allocAddress == null) {
			    System.err.println("Injector - Can not allocate memory for target process") ; 
		  }
		  
         shellCode.rebase((int)Pointer.nativeValue(allocAddress));
      
		   byte shellCodeBytes[] = shellCode.bytes() ; 
		     //System.out.println("ShellCode Size : " + shellCodeBytes.length) ; 
		     ProcessIO.write(processHandle, allocAddress, shellCodeBytes) ;  
		
		return allocAddress;
	}
}
