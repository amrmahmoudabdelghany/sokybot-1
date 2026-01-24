package org.sokybot.gameloader.injector.resolvers;

import org.sokybot.gameloader.expressions.ExpressionFactory;
import org.sokybot.gameloader.expressions.IExpression;


public class PResolver extends IResolver {


	public  PResolver(IResolver next ) {
		super(next) ; 
		
	}
	
   
	@Override
	protected IExpression handle(String express) {
		
		   
		  String command = express.substring(0,commandSBoundry) ; 
		 
		if(!command.equals("P")) { 
		 	//throw new RuntimeException("RResolver can not resolve this kind of expression "  + "'" + express + "'");   
		   if(next == null) { 
			   throw new RuntimeException("Unknow expression ' " + express + " ' ") ;  
		   }
		    return  next.resolve(express) ; 
		}
		
		
		
		return ExpressionFactory.createExpression(command,  express.substring(commandSBoundry + 1, commandEBoundry));
	}
}
