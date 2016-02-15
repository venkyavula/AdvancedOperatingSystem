

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class Application {

	public static Semaphore sem = new Semaphore(0);
	private static SystemClass sysObj;

	private static final int BUFFER_SIZE = 500;
	private static SctpChannel sctpChannel;
	private static InetSocketAddress serverSockAddr;
	private static String nodeName;
	private static int nodeNumber;
	private static int nodePort;
	private static int numberOfRequests;
	private static int delay;
	private static int totalNodes;
	private static LinkedList<Host> treeNodes = new LinkedList<Host>();
	private static int numberOftreeNodes = 0;
	
	private static SctpServerChannel sctpServerChannel;
	private static SctpChannel sctpCliChannel;
	private static InetSocketAddress socketAddr;
	
	private static int vector[];
	
	private static HashMap<Integer, Host> BCParent = new HashMap<Integer, Host>();
	private static HashMap<Integer, Integer> BCAckCount = new HashMap<Integer, Integer>();
	
	private static volatile boolean execution = false;
	private static String failureFileName = "failure.txt";
	
	// Always wrap FileWriter in BufferedWriter.
	private static BufferedWriter bufferedWriter;
	private static FileWriter failurefile;
	private static String testFile = "test.txt";

	public Application(String sysName , int sysPort)
	{
		try
		{
			sysObj = new SystemClass();
			readSpanningTreeInfo();
			startServer();
			makeRequest();
		} 
		catch(Exception ex)
		{
			System.out.println("Exception: Application Constructor"+nodeName+nodePort);
			ex.printStackTrace();
			
		}
	}

	public static void readSpanningTreeInfo()
	{	

		try
		{
			FileReader rdrObj = new FileReader("dest.txt");
			BufferedReader read = new BufferedReader(rdrObj);
			String s ="";
			s= read.readLine();
			while(s!=null){

				String array[] = s.split(" ");
				if (array[0].equals(InetAddress.getLocalHost().getHostName()))
				{
					nodeName = array[0];
					nodeNumber = Integer.parseInt(array[1]);
					nodePort = Integer.parseInt(array[2]);
					numberOfRequests = Integer.parseInt(array[3]);
					delay = Integer.parseInt(array[4]);
					totalNodes = Integer.parseInt(array[5]);
					vector = new int[totalNodes];
					for (int i = 0 ; i < vector.length ; i++)
					{
						vector[i] = 0;
					}
					
					for (int i = 6 ; i < array.length ; i++)
					{
						String node[] = array[i].split("-");
						Host tree_node = new Host();
						tree_node.setName(node[0]);
						tree_node.setapplicationPort(Integer.parseInt(node[1]));
						treeNodes.add(tree_node);
						numberOftreeNodes++;
					}
				}
				s=read.readLine();

			}
			read.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Exception: Application: Read Spanning Tree Info"+nodeName+nodePort);
			e.printStackTrace();
		}

	}

	public static void makeRequest() throws Exception
	{
		RandomVariables rv = new RandomVariables();
		for (int i =0 ; i < numberOfRequests ; i++)
		{

			double e = rv.exponential_rv(delay);
			long waitTime = (long)(e*1000000);


			TimeUnit.MICROSECONDS.sleep(waitTime);
			//send CS_ENTER Message to System
			sysObj.CS_ENTER();
			sem.acquire();
			//System.out.println("Application ACQUIRE"+nodeName);
			criticalSectionExecution();
			while(true)
			{
				if (!execution)
				{
					//System.out.println("WHILE BREAK");
					break;
				}
			}
			String content = ")" + nodeName + "-" + nodePort+" Request Number: "+vector[nodeNumber -1];
			writeToFile(testFile , content);
			sysObj.CS_EXIT();
		}
		System.out.println("DONE "+nodeName+"-"+nodePort);
	}

	public static void criticalSectionExecution()
	{
		//System.out.println("I got the Token.... Entering Critical Section.");
		String content = "(" + nodeName + "-" + nodePort+"  Request Number: "+(vector[nodeNumber -1] + 1);
		writeToFile(testFile , content);
		execution = true;
		Host node = new Host();
		node.setNull();
		BCParent.put(nodeNumber, node);
		BCAckCount.put(nodeNumber , 0);
		vector[nodeNumber -1]++;
		
		
		sendBroadcastMessage(nodeNumber ,get_vectorCount(vector) );
	}
	
	public static int get_vectorCount(int[] vec)
	{
		int sum = 0;
		for (int i = 0 ; i < vec.length ; i++)
		{	
			sum += vec[i];
		}
		return sum;
	}

	
	public static void startServer()
	{
		try
		{
			Thread applicationserverThread = new Thread(){
				public void run(){
					try {
						serverProgram();
					} catch (Exception e) {
						System.out.println("Exception Application-Server-Thread");
						e.printStackTrace();
					}
				}
			};
			applicationserverThread.start();
		}
		catch(Exception ex)
		{
			System.out.println("Exception: Application StartServer"+nodeName+nodePort);
			ex.printStackTrace();
		}
	}

	public static void client_sendMessage(Message msg, String ReceiverName , int reciverPortNo)
	{
		
		try
		{
			InetSocketAddress serverSockAddr = new InetSocketAddress(ReceiverName,reciverPortNo);
			sctpCliChannel = SctpChannel.open();
			sctpCliChannel.connect(serverSockAddr);
			ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
			byte[] data = objectToByte(msg);
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			byteBuffer.put(data);
			byteBuffer.flip();
			sctpCliChannel.send(byteBuffer,messageInfo);
			sctpCliChannel.close();
		}
		catch(ClosedChannelException e)
		{
			System.out.println("ClosedChannelException");
			client_sendMessage( msg,  ReceiverName ,  reciverPortNo);
		}
		catch(Exception ex)
		{
			System.out.println("Exception: Application ClientProgram"+nodeName+nodePort);
			ex.printStackTrace();

		}
	}

	public static void serverProgram()
	{
		try
		{
			sctpServerChannel = SctpServerChannel.open();
			socketAddr = new InetSocketAddress(nodePort);
			sctpServerChannel.bind(socketAddr); 
			
			System.out.println("Server Listening : "+ nodeName + nodePort);
			
			while(true)
			{
				sctpChannel = sctpServerChannel.accept();
				ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

				MessageInfo messageInfo = sctpChannel.receive(byteBuffer,null,null);
				byteBuffer.position(0);
				byteBuffer.limit(BUFFER_SIZE);
				byte[] bytes = new byte[byteBuffer.remaining()];
				byteBuffer.get(bytes, 0 , bytes.length);

				Message message = (Message)byteToObject(bytes);

				byteBuffer.clear(); // clear buffer to hold new contents
				byteBuffer.put(new byte[BUFFER_SIZE]);
				byteBuffer.clear();

				sctpChannel.shutdown();
				sctpChannel.finishConnect();
				sctpChannel.close();

				if ( (message.getType() == 1)|| (message.getType() == 2) ) // Received Broadcast Message  or Broadcast ACK
				{
					onReceiveBroadcastMessage(message);
				}
			}
		}
		catch(Exception ex)
		{
			System.out.println("Exception: Application ServerProgram"+nodeName+nodePort);
			ex.printStackTrace();

		}

	}
	
	public static void onReceiveBroadcastMessage(Message message)
	{
		Host parent_node = new Host();
		
		if (message.getType() == 1) //Broadcast Message
		{
			parent_node.setName(message.gethostName());
			parent_node.setapplicationPort(message.getapplicationportNo());
			BCAckCount.put(message.getInitiator(), 0);
			BCParent.put(message.getInitiator(), parent_node);
			//System.out.println(""+nodeName+"-"+nodePort+"The Broadcast Message: "+message.getInitiator()+" : "
				//	+message.getSum());
			
			if ( !(get_vectorCount(vector) < message.getSum()))
			{
				System.out.println("OVERLAP");
				String content = "Overlap of CS between nodes "+nodeNumber+" and "+message.getInitiator()+""
						+ " During the "+vector[nodeNumber -  1]+" th request of node "+nodeNumber;
				try {
					failurefile = new FileWriter(failureFileName, true);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				writeToFile(failureFileName , content);
				
			}
			vector[message.getInitiator() -  1]++;
					
			sendBroadcastMessage(message.getInitiator() , message.getSum()); // Forward broadcast msg to neighbors
		}
		else if (message.getType() == 2) //Broadcast ACK message
		{
			int cnt =  BCAckCount.get(message.getInitiator());
			cnt = cnt + 1;
			BCAckCount.put(message.getInitiator(), cnt);

		}
		
		parent_node = BCParent.get(message.getInitiator());
		
		if (parent_node.isNull()) //The node where broadcast message started
		{
			if (BCAckCount.get(message.getInitiator()) == numberOftreeNodes) // received BC ACKs from all its neighbors
			{
				//System.out.println(""+nodeName+"-"+nodePort+"BROADCAST COMPLETE!!!");
				execution = false;
				//send CS_EXIT Message to System
				
			}
		}
		else
		{
			if (BCAckCount.get(message.getInitiator()) == ( numberOftreeNodes - 1 ) )
			{
				//Back Trace Broadcast ACKs
				sendBroadcastACK(message.getInitiator());
			}
		}

	}

	public static void sendBroadcastMessage (int initiator , int sum)
	{
		/*Construct a broadcast message with the following arguments 
		 * 1. Message Type 2. Message 3. Sender Name 4 . Sender Port*/
		Message message = new Message(1,initiator,sum,nodeName,nodePort);
		
		Host parent_node = BCParent.get(initiator);

		for (int i = 0 ; i < numberOftreeNodes ; i++ )
		{
			if (!(parent_node.isNull()))
			{
				if ((parent_node.getName().equals(treeNodes.get(i).getName())) & (parent_node.getapplicationPort() == (treeNodes.get(i).getapplicationPort())))
				{
					continue;
				}

			}
			String hostname = treeNodes.get(i).getName();
			int port = treeNodes.get(i).getapplicationPort();

			client_sendMessage(message , hostname , port);
		}
	}

	public static void sendBroadcastACK (int initiator)
	{
		Host parent_node = BCParent.get(initiator);
		String hostname = parent_node.getName();
		int port = parent_node.getapplicationPort();
		Message msg = new Message(2,initiator , nodeName , nodePort );
		client_sendMessage(msg , hostname , port);	

	}
	
	public static byte[] objectToByte(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	public static Object byteToObject(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}
	public static synchronized void writeToFile(String fileName , String content)
	{

		try {
				FileWriter file = new FileWriter(fileName, true);
				bufferedWriter = new BufferedWriter(file);
				// Note that write() does not automatically
				// append a newline character.
				bufferedWriter.newLine();
				bufferedWriter.write(content);

				// Always close files.
				bufferedWriter.close();
			
		}
		catch(IOException ex) {
			System.out.println(
					"Error writing to file '"
							+ fileName + "'");
			ex.printStackTrace();
		}

	}
	
}
