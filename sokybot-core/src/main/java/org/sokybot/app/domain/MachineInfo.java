package org.sokybot.app.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MachineInfo implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	
	@ManyToOne
	private GroupInfo group ; 
	
	@Column(unique = true , nullable = false)
	private String machineName  =""; 
	
	//@Column(unique = true , nullable = true) 
	//private int settingId ; 
	
	
	public MachineInfo(GroupInfo group , String machineName) { 
		this.group = group ; 
		this.machineName = machineName ; 
	}
	
}
