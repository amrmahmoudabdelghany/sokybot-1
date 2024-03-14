package org.sokybot.app.domain;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	@Column(unique = true , nullable = false)
	private String name = "" ; 
	@Column(unique =  true , nullable =  false)
	private String gamePath = "" ; 
	
	
	public GroupInfo(String name , String gamePath) { 
		this.name = name ; 
		this.gamePath = gamePath ; 
	}
	
	
}
