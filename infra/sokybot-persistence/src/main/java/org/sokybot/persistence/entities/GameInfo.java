package org.sokybot.persistence.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameInfo {


	@Id
	private String gamePath ; 
	
	private int port ; 
	
	private int version ; 
	
	@OneToOne(cascade = CascadeType.ALL)
	private DivisionInfo divisionInfo ; 
	
	@OneToOne(cascade = CascadeType.ALL)
	private SilkroadType silkroadType ;
	
	public String getGamePath() {
		return gamePath;
	}
	
	public int getPort() {
		return port;
	}
	
	public int getVersion() {
		return version;
	}
	
	public DivisionInfo getDivisionInfo() {
		return divisionInfo;
	}
	
	public SilkroadType getSilkroadType() {
		return silkroadType;
	}
	
	public void setGamePath(String gamePath) {
		this.gamePath = gamePath;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}
	
	public void setDivisionInfo(DivisionInfo divisionInfo) {
		this.divisionInfo = divisionInfo;
	}
	
	public void setSilkroadType(SilkroadType silkroadType) {
		this.silkroadType = silkroadType;
	}
	
}
