

import java.io.Serializable;
import java.util.ArrayList;
/**
 * This class will be used to store the Host details
 * 
 *
 */
public class Host implements Serializable {
	
	 
	private static final long serialVersionUID = -1324235178280439563L;
	private String name;
	private int port;
	private int applicationPort;
	
	public Host()
	{
		this.name = null;
		this.port = 0;
		this.applicationPort = 0;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getapplicationPort() {
		return applicationPort;
	}
	public void setapplicationPort(int port) {
		this.applicationPort = port;
	}
	public void setNull(){
		this.name = null;
		this.port = 0;
		this.applicationPort = 0;
	}
	public boolean isNull(){
		if( (this.name == null) & (this.port == 0) & (this.applicationPort == 0))
			return true;
		else
			return false;
				
		
	
	}
	
}