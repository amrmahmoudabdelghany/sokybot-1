package org.sokybot.gameloader.injector.shellcode;

public interface IShellCodeBuilder {

	public IShellCodeBuilder defineParam(String paramName , byte[] paramValue) ; 
	public IShellCode build() ; 
}
