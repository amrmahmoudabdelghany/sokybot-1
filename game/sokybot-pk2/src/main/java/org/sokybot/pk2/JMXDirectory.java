/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sokybot.pk2;

import lombok.Data;

import java.util.List;


/**
 *
 * @author AMROO
 */
@Data
public class JMXDirectory {
	private String name;
	private long position;
	private List<JMXFile> jMXFiles = null;
	private List<JMXDirectory> subFolders = null;

	public JMXDirectory(String jmxFileName , long startLocation) {
		this.name = jmxFileName ;
		this.position = startLocation ;

	}





}
