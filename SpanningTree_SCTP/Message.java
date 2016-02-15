import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * This is pojo class for Message that is being transferred 
 * @author vxa141230
 *
 */
public class Message implements Serializable {

	private static final long serialVersionUID = 3884398797680355305L;
	int type;
	String message;
	Host sender;
	 
	public Host getSender() {
		return sender;
	}
	public void setSender(Host sender) {
		this.sender = sender;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	 
}
