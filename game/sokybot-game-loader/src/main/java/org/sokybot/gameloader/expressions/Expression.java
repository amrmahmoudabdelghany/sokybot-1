package org.sokybot.gameloader.expressions;

public class Expression implements IExpression {

	private String command ; 
	private String value ; 
	
	
	public Expression() {
	  command = null ; 
	  value = null ; 
	} 
	public Expression(String command , String value) { 
		this.command = command ; 
		this.value = value ; 
		
	}
	@Override
	public String command() {
	  return this.command ; 
	}
	
	@Override
	public String value() {
	  return this.value ; 
	}
	
	
}
