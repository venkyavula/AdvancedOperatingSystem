



import java.io.Serializable;

public class Message implements Serializable{
	private int messageType;
	private String message;
	private String hostName;
	private int portNo;
	private int applicationPort;
	private int initiator;
	private int sumOfVector;
	

	public Message (int type , String msg , String name , int port , int appPort)
	{
		messageType = type;
		message = msg;
		hostName = name;
		portNo = port;
		applicationPort = appPort;
	}
	public Message (int type , String msg , String name , int port )
	{
		messageType = type;
		message = msg;
		hostName = name;
		portNo = port;
		applicationPort = 0;
	}
	public Message (int type , int init , int sum , String name , int port )
	{
		messageType = type;
		hostName = name;
		portNo = 0;
		applicationPort = port;
		initiator = init;
		sumOfVector = sum;
	}
	public Message (int type , int init , String name , int port )
	{
		messageType = type;
		hostName = name;
		portNo = 0;
		applicationPort = port;
		initiator = init;
	}
	public Message(int type , String msg)
	{
		messageType = type;
		message = msg;
		hostName = null;
		portNo = 0;
		applicationPort = 0;
	}

	public int getType()
	{
		return messageType;
	}
	public String getMessage()
	{
		return message;
	}
	public String gethostName()
	{
		return hostName;
	}
	public int getportNo()
	{
		return portNo;
	}
	public int getapplicationportNo()
	{
		return applicationPort;
	}
	public int getInitiator()
	{
		return initiator;
	}
	public int getSum()
	{
		return sumOfVector;
	}

}
