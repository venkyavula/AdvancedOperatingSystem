

import java.net.InetSocketAddress;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import com.sun.nio.sctp.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SystemClass {

	private static final int BUFFER_SIZE = 500;
	private static int nodeNumber;
	private static  String nodeName;
	private static  int nodePort;
	private static int applicationPort;
	private static boolean initialNode;
	private static LinkedList<String> neighbourNodes = new LinkedList<String>();
	private static LinkedList<Host> treeNodes = new LinkedList<Host>();
	private static int numberOfNeighbours = 0;
	private static int numberOftreeNodes = 0;
	ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	private static SctpServerChannel sctpServerChannel;
	private static SctpChannel sctpChannel;
	private static SctpChannel sctpCliChannel;
	private static InetSocketAddress socketAddr;
	private static boolean requestReceived = false;
	private static volatile Host parent_node = new Host();
	private static int no_Of_ACKs = 0;
	private static int no_Of_NACKs = 0;
	private static int no_Of_Done = 0;
	private static int no_Of_BCACKs = 0;
	private static volatile boolean isTokenAvailable = false;
	private static volatile boolean CS_Execution = false;
	private static volatile Queue<Host> fifoQueue = new LinkedList<Host>();
	private static Application appObj;
	private static String testFile = "test.txt";
	private static String spanningTreeFile = "spanningTree.txt";
	private static BufferedWriter bufferedWriter;
	private static int requests;
	private static int delay;
	private static int totalNodes;
	private static final Object lock = new Object();

	public static void main (String args[]) 
	{
		startServer();
		configureNodes(args);
		startSpanningTree();
	}

	public static void configureNodes(String[] args)
	{

		nodeNumber = Integer.parseInt(args[0]);
		nodeName = args[1];
		nodePort = Integer.parseInt(args[2]);
		applicationPort = Integer.parseInt(args[3]);
		if (args.length > 4)
		{
			String[] neighbours = args[4].split(",");
			for (int i = 0 ; i < neighbours.length ; i++)
			{
				neighbourNodes.add(neighbours[i]);
				numberOfNeighbours++;
			}
		}
		requests = Integer.parseInt(args[5]);
		delay = Integer.parseInt(args[6]);
		totalNodes = Integer.parseInt(args[7]);
		if (args.length > 8)
		{
			initialNode = true;
		}
		else
		{
			initialNode = false;
		}

	}

	public static void startSpanningTree()
	{
		if (initialNode == true)
		{
			try{
				TimeUnit.SECONDS.sleep(3);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				System.out.println("Exception: startSpanningTree");
			}

			requestReceived = true;
			isTokenAvailable = true;
			parent_node.setNull();
			/*Construct a message with the following arguments 
			 * 1. Message Type 2. Message 3. Sender Name 4 . Sender Port*/
			Message msg = new Message(0,"REQUEST",nodeName,nodePort,applicationPort); 

			for(int i = 0 ; i < numberOfNeighbours ; i++)
			{
				String[] info = neighbourNodes.get(i).split("-");
				String hostname = info[0];
				int port = Integer.parseInt(info[1]);
				client_sendMessage(msg , hostname , port);

			}
			
			try{
				TimeUnit.SECONDS.sleep(5);
				appObj = new Application(nodeName , nodePort);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				System.out.println("Exception: main");
			}



		}
		else
		{
			try{
				TimeUnit.SECONDS.sleep(8);
				appObj = new Application(nodeName , nodePort);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
				System.out.println("Exception: main");
			}
		}
	}

	public static void startServer()
	{
		try
		{
			Thread serverThread = new Thread(){
				public void run(){
					try {
						serverProgram();
					} catch (Exception e) {
						System.out.println("Exception-Server-Thread");
						e.printStackTrace();
					}
				}
			};
			serverThread.start();
		}
		catch(Exception ex)
		{
			System.out.println("Exception: StartServer");
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
			// retry
			client_sendMessage( msg,  ReceiverName ,  reciverPortNo);
		}
		catch(Exception ex)
		{
//System.out.println("Exception: ClientProgram"+"ReceiverName"+ ReceiverName+"port"+reciverPortNo+"isToken"+isTokenAvailable+"node"+nodeName+" port"+nodePort );
//			ex.printStackTrace();

		}
	}

	public static void serverProgram()
	{
		try
		{
			sctpServerChannel = SctpServerChannel.open();
			socketAddr = new InetSocketAddress(nodePort);
			sctpServerChannel.bind(socketAddr); 

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

				if (message.getType() == 0)
				{
					spanningTreeMessage(message);
				}
				else if (message.getType() == 4)
				{
					raymondsMessage(message);
				}
			}
		}
		catch(Exception ex)
		{
			System.out.println("Exception: ServerProgram");
			ex.printStackTrace();

		}

	}

	public static void spanningTreeMessage(Message message)
	{
		String str = message.getMessage();

		//Received a request for the first time or received positive ACK - Add it as Tree neighbor
		if( ((message.getType() == 0) && (str.equals("REQUEST")) && requestReceived == false ) ||
				((message.getType() == 0) && (str.equals("ACK"))) )
		{
			Host node = new Host();
			node.setName(message.gethostName());
			node.setPort(message.getportNo());
			node.setapplicationPort(message.getapplicationportNo());
			treeNodes.add(node); // Add tree Neighbor
			numberOftreeNodes++; // Increment the number of tree neighbors
			System.out.println(""+nodeName+"-"+nodePort+"Tree Node "+numberOftreeNodes+" : "+node.getName()+""
					+ "-"+node.getPort());
		}
		if ((message.getType() == 0) && (str.equals("REQUEST")) && requestReceived == true)
		{
			// Send Negative Acknowledgement
			Message nackMessage = new Message (0,"NACK",nodeName,nodePort);
			client_sendMessage(nackMessage, message.gethostName(),message.getportNo());
		}
		else if ((message.getType() == 0) && (str.equals("REQUEST")) && requestReceived == false)
		{
			// send Positive Acknowledgement
			requestReceived = true;
			parent_node.setName(message.gethostName());
			parent_node.setPort(message.getportNo());
			Message ackMessage = new Message (0,"ACK",nodeName,nodePort,applicationPort);
			client_sendMessage(ackMessage,message.gethostName(),message.getportNo());
			//Forward request to its neighbors
			forwardSpanningRequest(message.gethostName(),message.getportNo());

		}
		if ((message.getType() == 0) && (str.equals("ACK")))
		{
			no_Of_ACKs++;
		}
		else if ((message.getType() == 0) && (str.equals("NACK")))
		{
			no_Of_NACKs++;
		}
		else if ((message.getType() == 0) && (str.equals("DONE")))
		{
			no_Of_Done++;
		}

		if (parent_node.isNull()) // The node where Spanning tree started
		{
			if ( (numberOfNeighbours == (no_Of_ACKs + no_Of_NACKs)) && (no_Of_Done == no_Of_ACKs) )
			{
				//Display user a spanning tree done message and get ready for broadcast
				System.out.println (""+nodeName+"-"+nodePort+"Spanning Tree Done");
				writeSpanningTreeToFile();
				mergeFiles();
			}
		}

		else 
		{
			if ( ((numberOfNeighbours - 1) == (no_Of_ACKs + no_Of_NACKs)) && (no_Of_Done == no_Of_ACKs) )
			{
				//Send Spanning tree done message and get ready for broadcastS
				String hostname = parent_node.getName();
				int port = parent_node.getPort();
				/*Construct a message with the following arguments 
				 * 1. Message Type 2. Message 3. Sender Name 4 . Sender Port*/
				Message msg = new Message(0,"DONE",nodeName,nodePort); 
				writeSpanningTreeToFile();
				client_sendMessage(msg , hostname , port);

			}
		}
	}
	public static void forwardSpanningRequest(String senderName , int senderPort)
	{
		String sender = senderName+"-"+senderPort;
		for (int i = 0 ; i < numberOfNeighbours ; i++)
		{			
			if ( !(neighbourNodes.get(i).equals(sender)) )
			{
				String[] info = neighbourNodes.get(i).split("-");
				String hostname = info[0];
				int port = Integer.parseInt(info[1]);
				/*Construct a message with the following arguments 
				 * 1. Message Type 2. Message 3. Sender Name 4 . Sender Port*/
				Message msg = new Message(0,"REQUEST",nodeName,nodePort,applicationPort); 
				client_sendMessage(msg , hostname , port);
			}
		}
	}
	public static void writeSpanningTreeToFile()
	{
		System.out.println("File Written "+nodeName+nodePort);
		String treeNeigh = new String();
		Host node = new Host();
		for (int i = 0 ; i < numberOftreeNodes ; i++)
		{
			node = treeNodes.get(i);
			treeNeigh += node.getName()+"-"+node.getapplicationPort()+" ";
		}
		String message = ""+nodeName+" "+nodeNumber+" "+applicationPort+" "+requests+" "+delay+" "+totalNodes+" "+treeNeigh;
		String stFile = nodeName+"_"+spanningTreeFile;
        writeToFile(stFile,message);

	}
	public static void raymondsMessage(Message message)
	{
		if (message.getMessage().equals("REQUEST"))
		{
			writeToFile("node"+nodeNumber+".txt","REQUEST MESSAGE");
			onReceiveTokenRequest(message.gethostName(), message.getportNo());
		}
		else if (message.getMessage().equals("GRANT"))
		{
			synchronized(lock)
			{
				isTokenAvailable = true;
			}
			writeToFile("node"+nodeNumber+".txt","GRANT MESSAGE");
			try{
				onReceiveTokenGrant();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

		}
		else if (message.getMessage().equals("GRANT_REQUEST"))
		{
			synchronized(lock)
			{
				isTokenAvailable = true;
			}
			writeToFile("node"+nodeNumber+".txt","GRANT MESSAGE");
			onReceiveTokenGrant_Request(message.gethostName(), message.getportNo());
		}
	}

	public static void onReceiveTokenRequest(String requesterName , int requesterPort)
	{

		synchronized(lock)
		{
			if ( ((fifoQueue.size()) > 0 ) || ((isTokenAvailable) & (CS_Execution)) )
			{
				writeToFile("node"+nodeNumber+".txt",("onReceiveTokenRequest : "+nodeName+" - "+nodePort+" "
						+ "FROM : "+requesterName+"- "+requesterPort+" : ADDED TO QUEUE"));
				Host node = new Host();
				node.setName(requesterName);
				node.setPort(requesterPort);
				fifoQueue.add(node);
			}
			else
			{
				if ((isTokenAvailable) & (!CS_Execution))
				//if (!CS_Execution)
				{
					writeToFile("node"+nodeNumber+".txt",("onReceiveTokenRequest : "+nodeName+" - "+nodePort+" "
							+ "FROM : "+requesterName+"- "+requesterPort+" : TOKEN AVAILABLE"+isTokenAvailable));
					Message message = new Message(4 , "GRANT" , nodeName , nodePort);
					client_sendMessage(message ,requesterName ,  requesterPort);

					{
						isTokenAvailable = false;
					}
					parent_node.setName(requesterName);
					parent_node.setPort(requesterPort);
				}
				else 
				{
					writeToFile("node"+nodeNumber+".txt",("onReceiveTokenRequest : "+nodeName+" - "+nodePort+" :"
							+ "FROM : "+requesterName+"- "+requesterPort+" NO TOKEN - SO TOKENREQUESTED TO PARENT"+isTokenAvailable));
					requestToken(requesterName , requesterPort);
				}
			}
		}

	}

	public static void onReceiveTokenGrant() throws Exception
	{
		


		if (isTokenAvailable)
		{
			Host requester = fifoQueue.poll();
		if (requester != null)
		{

			if ( (requester.getName().equals(nodeName)) & (requester.getPort() == nodePort) )
			{
				synchronized(lock)
				{
					isTokenAvailable = true;
					CS_Execution = true;
				}
				parent_node.setNull();
				
				appObj.sem.release();
				writeToFile("node"+nodeNumber+".txt",("onReceiveTokenGrant : "+nodeName+" - "+nodePort+" : CSEXECUTION"));
			}
			else
			{
				synchronized(lock)
				{
					isTokenAvailable = false;
				}
				writeToFile("node"+nodeNumber+".txt",("onReceiveTokenGrant : "+nodeName+" - "+nodePort+" : GRANT FORWARD"));
				parent_node.setName(requester.getName());
				parent_node.setPort(requester.getPort());
				sendGrantMessage(requester);
			}
		}
		else
		{
			writeToFile("node"+nodeNumber+".txt",("No more requests in queue : "+nodeName+" - "+nodePort+" "
					+ ": I have the token"));
			
			
		}
		}
	}
	public static void sendGrantMessage(Host node)
	{
		synchronized(lock)
		{
			isTokenAvailable = false;
		}
		Message message;
		if (fifoQueue.size() > 0 )
		{
			writeToFile("node"+nodeNumber+".txt",("sendGrantMessage : "+nodeName+" - "+nodePort+""
					+ " : GRANT_REQUEST TO : "+node.getName()+"- "+node.getPort()));
			message = new Message(4 , "GRANT_REQUEST" , nodeName , nodePort);

		}
		else
		{
			writeToFile("node"+nodeNumber+".txt",("sendGrantMessage : "+nodeName+" - "+nodePort+" : GRANT"
					+ "TO : "+node.getName()+"- "+node.getPort()));
			message = new Message(4 , "GRANT" , nodeName , nodePort);
		}
		
		


		client_sendMessage(message , node.getName() , node.getPort());
	}
	public static void onReceiveTokenGrant_Request(String requesterName , int requesterPort)
	{
		Host node = new Host();
		node.setName(requesterName);
		node.setPort(requesterPort);
		fifoQueue.add(node);

		writeToFile("node"+nodeNumber+".txt",("onReceiveTokenGrant_Request : "+nodeName+" - "+nodePort+""
				+ "FROM : "+requesterName+"- "+requesterPort+ " : REQUEST ADDED TO QUEUE"));

		try{
			onReceiveTokenGrant();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	public static void requestToken(String requesterName , int requesterPort)
	{


		Host node = new Host();
		node.setName(requesterName);
		node.setPort(requesterPort);
		fifoQueue.add(node);

		writeToFile("node"+nodeNumber+".txt",("requestToken : "+nodeName+" - "+nodePort+" : REQUEST ADDED TO QUEUE"));

		if (fifoQueue.size() == 1)
		{

			Message message = new Message(4 , "REQUEST" , nodeName , nodePort);
			String tokenHolder_name = parent_node.getName();
			int tokenHolder_port = parent_node.getPort();
			client_sendMessage(message ,tokenHolder_name ,  tokenHolder_port);
			writeToFile("node"+nodeNumber+".txt",("requestToken : "+nodeName+" - "+nodePort+" : TOKEN REQUEST SENT TO PARENT"
					+ ": "+tokenHolder_name+" - "+ tokenHolder_port));
		}
	}

	public static void CS_ENTER() throws Exception
	{

		synchronized(lock){

			if ( (fifoQueue.size()) > 0 )
			{
				Host node = new Host();
				node.setName(nodeName);
				node.setPort(nodePort);
				fifoQueue.add(node);
			}
			else
			{

				if ( (isTokenAvailable == true) & (!CS_Execution) )
				{
					writeToFile("node"+nodeNumber+".txt",("CS_ENTER : "+nodeName+" - "+nodePort+" : TOKEN AVAILABLE"));
					
					appObj.sem.release();
					CS_Execution = true;
				}
				else
				{
					writeToFile("node"+nodeNumber+".txt",("CS_ENTER : "+nodeName+nodePort+" : TOKEN REQUESTED"));
					requestToken(nodeName , nodePort);
				}
			}
		}

	}
	public static void CS_EXIT()               
	{
		CS_Execution = false;
		writeToFile("node"+nodeNumber+".txt",(")"+nodeName+nodePort+" : "));
		try{
			onReceiveTokenGrant();
			onReceiveTokenGrant(); // To avoid Problem 1

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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

			
				// Always wrap FileWriter in BufferedWriter.
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
	
	 private static void mergeFiles(){
         File folder = new File(".");
         File[] listOfFiles = folder.listFiles();
         File fObj = new File("dest.txt");
         try{
         FileWriter fstream = new FileWriter(fObj, true);
         BufferedWriter out = new BufferedWriter(fstream);

             for (int i = 0; i < listOfFiles.length; i++) {
               if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(spanningTreeFile)) {
                 String fileName = listOfFiles[i].getAbsolutePath();
                 FileReader rdrObj = new FileReader(fileName);
                         BufferedReader read = new BufferedReader(rdrObj);
                         String s ="";
                         s= read.readLine();
                         while(s!=null){
                                 //System.out.println(s);
                                 out.write(s);
                                 out.newLine();
                                 s=read.readLine();
                         }
                 read.close();
               }
             }
         out.close();
                 } catch (IOException e) {
         // TODO Auto-generated catch block
         System.out.println("Exception");
         e.printStackTrace();
 }

 }

	
	
	


}



