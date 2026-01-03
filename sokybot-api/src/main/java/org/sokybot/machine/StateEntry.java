package  org.sokybot.machine; 

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.statemachine.annotation.OnStateChanged;
import org.springframework.statemachine.annotation.OnStateEntry;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@OnStateEntry
public @interface StateEntry {

	MachineState[] source() default {};

	MachineState[] target() default {};

}
