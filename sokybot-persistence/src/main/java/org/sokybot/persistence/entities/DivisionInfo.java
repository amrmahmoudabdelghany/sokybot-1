package org.sokybot.persistence.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@ToString
@NoArgsConstructor
public class DivisionInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id ; 
	
	
	public byte local = 0;
	
	@OneToMany(fetch = FetchType.EAGER , cascade = CascadeType.ALL)
	private  List<Division> divisions;

	

	public void addDivision(Division division) {
		if(this.divisions == null) { 
			this.divisions = new ArrayList<>() ; 
		}
		
		this.divisions.add(division);
	}

	public List<Division> getDivisions() {
		return this.divisions;
	}

	public Division getDivision(String divsionName) {

		for (Division d : divisions) {
			if (d.getName().equals(divsionName))
				return d;
		}
		return null;
	}

	public Division getDivisionForHost(String host) {

		for (Division d : divisions) {
			List<String> hosts = d.getHosts();
			for (String dHost : hosts) {
				if (dHost.equalsIgnoreCase(host))
					return d;
			}
		}
		return null;
	}

	public String[] getDivisionNames() {

		String res[] = new String[divisions.size()];
		for (int i = 0; i < res.length; i++) {
			res[i] = divisions.get(i).getName();
		}
		return res;
	}

}
