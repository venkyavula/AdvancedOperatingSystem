import java.io.Serializable;
import java.util.ArrayList;
/**
 * This class will be used to store the Host details
 * @author vxa141230
 *
 */
public class Host implements Serializable {
	
	 
	private static final long serialVersionUID = -1324235178280439563L;
	int id;
	String name;
	int port;
	ArrayList<Integer> neighbours = new ArrayList<Integer>();
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
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public ArrayList<Integer> getNeighbours() {
		return neighbours;
	}
	public void setNeighbours(ArrayList<Integer> neighbours) {
		this.neighbours = neighbours;
	}
	
}
