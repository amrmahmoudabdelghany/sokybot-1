package org.sokybot.persistence.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@ToString
@NoArgsConstructor
public class Division implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	@Column(unique = true , nullable = false)
	private String name;
	
	@ElementCollection(fetch = FetchType.EAGER )
	private  List<String> hosts  = new ArrayList<>();

	

	public void addHost(String host) {
		if(this.hosts == null) { 
			this.hosts = new ArrayList<>() ; 
		}
		this.hosts.add(host);
	}

	public List<String> getHosts() {
		return new ArrayList<>(this.hosts) ; 
	}

	public String getName() { 
		return this.name ; 
	}
	public void setName(String name) { 
		this.name = name ; 
	}
	public String getRandomHost() {
		if (this.hosts != null) {
			Random rnd = new Random();

			int pos = rnd.nextInt(this.hosts.size());
			return this.hosts.get(pos);
		}
		return null;
	}

	public boolean isExistsHost(String host) {
		for (String h : hosts) {
			if (h.equalsIgnoreCase(host)) {
				return true;
			}
		}
		return false;
	}

}
