package org.sokybot.persistence.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@Getter
@ToString
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
public class SilkroadEntity implements Serializable{
	
	
	@Id
	private int refId;
	
	@Column( nullable =  false)
	private String longId;
	
	private String name;

	public int getRefId() {
		return refId;
	}

	public String getLongId() {
		return longId;
	}

	public String getName() {
		return name;
	}
}
