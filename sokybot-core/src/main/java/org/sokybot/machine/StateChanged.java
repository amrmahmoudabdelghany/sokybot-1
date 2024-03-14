package org.sokybot.machine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.statemachine.annotation.OnStateChanged;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@OnStateChanged
public @interface StateChanged {


	MachineState[] source() default {} ; 
	
	MachineState[] target() default {} ; 
	
	
}
