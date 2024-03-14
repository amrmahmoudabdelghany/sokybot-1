package org.sokybot;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT.HANDLE;

import com.sun.jna.ptr.IntByReference;


public interface IKernel32 extends Library  {

	//,  W32APIOptions.ASCII_OPTIONS
	IKernel32 INSTANCE = Native.load("kernel32",
			IKernel32.class   );

	DWORD ResumeThread(HANDLE hThread);
	 boolean CreateProcessA(String lpApplicationName, 
			 byte[] lpCommandLine,
	            WinBase.SECURITY_ATTRIBUTES lpProcessAttributes,
	            WinBase.SECURITY_ATTRIBUTES lpThreadAttributes,
	            boolean bInheritHandles, DWORD dwCreationFlags,
	            Pointer lpEnvironment, String lpCurrentDirectory,
	            WinBase.STARTUPINFO lpStartupInfo,
	            WinBase.PROCESS_INFORMATION lpProcessInformation);
	 
	  boolean VirtualProtectEx(HANDLE hProcess ,
			  Pointer lpAddress ,
			  int dwSize  ,
			  DWORD flNewProtect ,
			  DWORDByReference lpflOldProtect) ; 
	  
	  //Pointer GetProcAddress(HMODULE hM ,String funName ) ; 
	 // IntByReference GetProcAddress(HMODULE dllHandle, String functionName);
	
	  boolean WriteProcessMemory(HANDLE hProcess,
			  Pointer lpBaseAddress,
			  Pointer lpBuffer, 
			  int nSize, 
			  IntByReference lpNumberOfBytesWritten);
	  boolean ReadProcessMemory(HANDLE hProcess,
			  Pointer lpBaseAddress,
			  Pointer lpBuffer,
			  int nSize,
			  IntByReference lpNumberOfBytesRead);
	  
	  HANDLE OpenProcess(int fdwAccess, boolean fInherit, int IDProcess);
	  boolean TerminateProcess(HANDLE hProcess, int uExitCode);
	  boolean CloseHandle(HANDLE hObject);
}
