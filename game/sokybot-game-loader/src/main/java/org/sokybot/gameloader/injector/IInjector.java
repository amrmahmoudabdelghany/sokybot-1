package org.sokybot.gameloader.injector;

import org.sokybot.gameloader.injector.shellcode.IShellCode;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;



public interface IInjector {

	public Pointer inject(HANDLE processHandle  , IShellCode shellCode) ; 
	
}
