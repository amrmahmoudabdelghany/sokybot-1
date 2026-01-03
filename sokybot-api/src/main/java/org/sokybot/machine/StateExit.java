package  org.sokybot.machine; 

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.statemachine.annotation.OnStateChanged;
import org.springframework.statemachine.annotation.OnStateEntry;
import org.springframework.statemachine.annotation.OnStateExit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@OnStateExit
public @interface StateExit {

	MachineState[] source() default {};

	MachineState[] target() default {};

}
