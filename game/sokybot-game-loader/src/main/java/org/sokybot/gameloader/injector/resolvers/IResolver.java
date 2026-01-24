package org.sokybot.gameloader.injector.resolvers;

import org.sokybot.gameloader.expressions.IExpression;

public abstract class IResolver {
 
 protected IResolver next  = null ; 
 protected int commandSBoundry = -1 ; 
 protected int commandEBoundry =  -1 ; 
 
	
 public IResolver(IResolver next) {  
	 this.next = next ; 
 }
	
	
   protected abstract IExpression handle(String expression) ; 
   
   protected void checkBoundries(String express) { 

		 commandSBoundry = express.indexOf('{') ; 
		
	     if(commandSBoundry == -1) { 
				throw new RuntimeException("Unvalid expression " + "'" + express + "'" +  " , Expression must enclosed within {}") ; 
				 
		 }
	      commandEBoundry = express.indexOf('}');
			
		 if(commandEBoundry == -1) { 
				throw new RuntimeException("Unvalid expression " + "'" + express + "'" +  " , Expression must enclosed within {}") ; 
				 
		 }
		 
		
   }
   
   public IExpression resolve(String expression) { 
	   
	   if(expression.isBlank()) return null ;  
	     checkBoundries(expression);
	   return handle(expression) ; 
   }
}
