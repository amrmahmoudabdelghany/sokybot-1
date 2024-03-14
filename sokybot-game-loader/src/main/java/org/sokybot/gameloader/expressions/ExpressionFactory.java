package org.sokybot.gameloader.expressions;

public class ExpressionFactory {

	
	
	public static IExpression createExpression(String command , String value) {

		return new Expression(command, value ) ;
	}
}
