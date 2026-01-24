package org.sokybot.gameloader.injector.shellcode;

public interface IShellCode {


	public int putArr(byte[] arr) ; 
	public int putString(String str) ; 
	public int putInt(int val) ;
	
	public int getCodeEntryPoint() ; 
	public void assignPtr(String varName , int val) ; 
	public void assignValue(String varName , int val) ; 
	public void rebase(int newBase) ; 
	public byte[] bytes() ; 
	public int size() ; 
	
	public void dump() ; 
	
}
