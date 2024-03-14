package org.sokybot.gameloader;

import org.sokybot.IKernel32;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;



public class ProcessIO {

	
	
	public  static byte [] read(HANDLE hProcess , Pointer address , int len) { 
		byte [] patch = new byte [len] ; 
		DWORDByReference oldProtection  = new DWORDByReference() ; 
		if(!IKernel32.INSTANCE.VirtualProtectEx(hProcess,
				  address,
				  patch.length,
				  new DWORD(0x40),
				  oldProtection)) {
			System.out.println("Can not change protection to write into process`s space") ; 
		}
		Memory mem = new Memory(patch.length) ; 
		IntByReference readedBytes = new IntByReference() ; 
		if(!IKernel32.INSTANCE.ReadProcessMemory(hProcess, address, mem, len, readedBytes)) {
		  System.out.println("Can not read process memeory ") ; 
		  
		}else { 
			mem.read(0, patch, 0, patch.length);
		}
		
		
		if(!IKernel32.INSTANCE.VirtualProtectEx(hProcess,
				  address,
				  patch.length,
				  oldProtection.getValue(),
				   oldProtection)) { 
			System.out.println("Can not restore protection to the writen process`s space") ; 
		}
		
		
		
		return patch ; 
		
	}
	public static boolean write(HANDLE hProcess , Pointer address ,byte [] patch) {
		DWORDByReference oldProtection  = new DWORDByReference() ; 
		boolean res = false ; 
		if(!IKernel32.INSTANCE.VirtualProtectEx(hProcess,
				  address,
				  patch.length,
				  new DWORD(0x40),
				  oldProtection)) {
			System.out.println("Can not change protection to write into process`s space") ; 
		}
		 Memory mem = new Memory(patch.length) ; 
		 mem.write(0, patch, 0, patch.length);
		  IntByReference numberOfBytes = new IntByReference() ; 
			 boolean ress =  IKernel32.INSTANCE.WriteProcessMemory(hProcess,
				   address,
					   mem,
					   patch.length,
					   numberOfBytes);
			 if(!ress) System.out.println("Can not write patch") ;
			 else {
				 System.out.println("Writen bytes : " + numberOfBytes.getValue()) ; 
			 }
		if(!IKernel32.INSTANCE.VirtualProtectEx(hProcess,
				  address,
				  patch.length,
				  oldProtection.getValue(),
				   oldProtection)) { 
			System.out.println("Can not restore protection to the writen process`s space") ; 
		}
		
		return res ; 
	}

}
