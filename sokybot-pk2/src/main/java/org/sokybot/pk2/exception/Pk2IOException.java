package org.sokybot.pk2.exception;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.sokybot.pk2.JMXFile;

import lombok.Getter;
// TODO write document to Pk2IOException class 
@Getter
public class Pk2IOException extends UncheckedIOException {

	private String targetPath ; 
	
	public Pk2IOException(String message,String targetPath ,  IOException ex) {
	     super(message, ex) ; 
	     this.targetPath = targetPath ; 
	     
	}

}
