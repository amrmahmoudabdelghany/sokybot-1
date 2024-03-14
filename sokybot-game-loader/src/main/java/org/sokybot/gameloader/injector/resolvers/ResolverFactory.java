package org.sokybot.gameloader.injector.resolvers;

public class ResolverFactory {

	
	
	public static IResolver createResolversChain() {
		 
		IResolver resolver = new CounterResolver(null) ; 
		resolver = new PResolver(resolver) ; 
		resolver = new RResolver(resolver) ; 
	    return resolver ; 	
		
	}
}
