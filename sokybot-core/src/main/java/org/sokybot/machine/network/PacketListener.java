package org.sokybot.machine.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketListener {
	int[] opcode() default {};
	
	PacketSource packetSource() default PacketSource.BOTH ; 
	
	public static enum PacketSource { 
		SERVER , 
		CLIENT , 
		BOTH ;
	}
}
