import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

/**
 * This class will be the client for this Broadcast service and will do all
 * sending messges of all types from a node.
 * 
 * @author vxa141230
 *
 */

public class SClient {

	Host server = null;
	public static final int MESSAGE_SIZE = 1000;
	ObjectOutputStream out;
	BufferedReader in;
	Host currentHost = new Host();
	List<Host> hostList = new ArrayList<Host>();
	HashMap<String, Host> hostmap = new HashMap<String, Host>();
	boolean isDesignated = false;
	boolean isReqSent = false;
	boolean isTreeReadyMsg = false;
	Host treePrnt = null;
	Host bcPrnt = null;
	int rcvBCDone = 0;
	int sentReqs = 0;
	int rcvReqs = 0;
	int recvACKs = 0;
	int recvNACKs = 0;
	int sentACKs = 0;
	int sentNACKs = 0;
	int recvTRs = 0;
	boolean isStartNode = false;
	boolean isBCstartnode = false;
	boolean isFirstBCOver = false;

	public boolean isBCstartnode() {
		return isBCstartnode;
	}

	public void setBCstartnode(boolean isBCstartnode) {
		this.isBCstartnode = isBCstartnode;
	}

	public int getRecvTRs() {
		return recvTRs;
	}

	public void setRecvTRs(int recvTRs) {
		this.recvTRs = recvTRs;
	}

	static boolean isAckSent = false;
	List<Host> treeNeighbours = new ArrayList<Host>();
	List<Host> treeBackup = new ArrayList<Host>();

	public int getRecvACKs() {
		return recvACKs;
	}

	public void setRecvACKs(int recvACKs) {
		this.recvACKs = recvACKs;
	}

	public int getRecvNACKs() {
		return recvNACKs;
	}

	public void setRecvNACKs(int recvNACKs) {
		this.recvNACKs = recvNACKs;
	}

	public int getSentACKs() {
		return sentACKs;
	}

	public void setSentACKs(int sentACKs) {
		this.sentACKs = sentACKs;
	}

	public int getSentNACKs() {
		return sentNACKs;
	}

	public void setSentNACKs(int sentNACKs) {
		this.sentNACKs = sentNACKs;
	}

	public Host getBcPrnt() {
		return bcPrnt;
	}

	public void setBcPrnt(Host bcPrnt) {
		this.bcPrnt = bcPrnt;
	}

	public int getRcvBCDone() {
		return rcvBCDone;
	}

	public void setRcvBCDone(int rcvBCDone) {
		this.rcvBCDone = rcvBCDone;
	}

	public static void main(String[] args) {
		SCTPTServer ser = new SCTPTServer(Integer.parseInt(args[0]));
		ser.start();
	}
	/**
	 * This method will send REQ message to the neighbours apart from its sender .
	 * @param host
	 * @param port
	 */
	public void sendReq(String host, int port, boolean isDesignated)
			throws UnknownHostException, IOException {
		// System.out.println("Client.sendReq()---------{"+host+""+port);
		String thisHost = getServer().getName() + "-" + getServer().getPort();
		Host clientInCof = hostmap.get(thisHost);

		if (!isDesignated) {
			if (!isAckSent) {
				isAckSent = true;
				sentACKs++;
				sendAck(host, port);
			} else {
				sentNACKs++;
				sendnack(host, port);
			}

			checkAndsendTreeReady();
		}
		if (!isReqSent) {
			isReqSent = true;

			if (clientInCof != null)
				for (Integer neighbor : clientInCof.getNeighbours()) {
					Host currNgbr = hostList.get(neighbor - 1);
					// System.out.println(currNgbr.getName()+"-----"+currNgbr.getPort());
					if (!currNgbr.getName().equals(host))// send REQ messages
															// apart from the
															// sender of the REQ
															// Message
					{

						ByteBuffer byteBuffer = ByteBuffer
								.allocate(MESSAGE_SIZE);
						Message reqMsg = new Message();
						reqMsg.type = 0;// request
						reqMsg.message = "Message";
						reqMsg.setSender(getServer());
						this.sentReqs++;
						sendMessage(currNgbr.getName(), currNgbr.getPort(),
								byteBuffer, reqMsg);
						// socket.close();
					}
				}
		}

		// System.out.println("Client.sendReq()-------}");

	}

	public int getSentReqs() {
		return sentReqs;
	}

	public void setSentReqs(int sentReqs) {
		this.sentReqs = sentReqs;
	}

	public int getRcvReqs() {
		return rcvReqs;
	}

	public void setRcvReqs(int rcvReqs) {
		this.rcvReqs = rcvReqs;
	}

	public void checkAndsendTreeReady() throws UnknownHostException,
			IOException {
		// System.out.println("Client.checkAndsendTreeReady()-----{");
		if (isBranchesRdy()) {
			if (getRecvTRs() == this.treeNeighbours.size())// if neighbors and
															// Tree Ready
															// messages count s
															// equal it can send
															// TR message to its
															// parent
				sendTreeReadyMsg();
		}

		// System.out.println("Client.checkAndsendTreeReady()-----}");

	}

	private void sendTreeReadyMsg() throws UnknownHostException, IOException {

		// System.out.println("Client.sendTreeReadyMsg()-------------{");
		if (treePrnt != null && !isTreeReadyMsg) {
			//

			isTreeReadyMsg = true;
			ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
			Message msg = new Message();
			msg.type = 3;// Tree Ready
			msg.message = "Tree Ready Message";
			msg.setSender(getServer());
			sendMessage(treePrnt.getName(), treePrnt.getPort(), byteBuffer, msg);
		} else if (!isTreeReadyMsg && isStartNode && getRecvTRs() > 0)// It must
																		// be
																		// designated
																		// root
		{
			System.out.println("Announcing Spnning tree is ready. ");
			this.treeBackup = this.treeNeighbours;
		}

		// System.out.println("Client.sendTreeReadyMsg()-------------}");
	}

	public Host getServer() {
		return server;
	}

	public void setServer(Host server) {
		this.server = server;
	}
	/**
	 * This method will send ACK message
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void sendAck(String host, int port) throws UnknownHostException,
			IOException {
		// System.out.println("1.1:Client.sendAck() from this"+getServer().getName()
		// + " ------{"+host+""+port);
		treePrnt = new Host();
		treePrnt.setName(host);
		treePrnt.setPort(port);
		System.out.println("TREE NEIGHBOUR --> " + host);
		// ***************************************
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		Message msg = new Message();
		msg.type = 1;// ACK
		msg.message = "ACK Message";
		msg.setSender(getServer());
		sendMessage(host, port, byteBuffer, msg);

	}
/**
 * This method will send the message to the particular host using SCTP
 * @param host
 * @param port
 * @param byteBuffer
 * @param msg
 */
	private void sendMessage(String host, int port, ByteBuffer byteBuffer,
			Message msg) {
		try {
			SocketAddress socketAddress = new InetSocketAddress(host, port);
			SctpChannel sctpChannel = SctpChannel.open();
			sctpChannel.connect(socketAddress);
			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
			byteBuffer.put(convertToBytes(msg));
			byteBuffer.flip();
			sctpChannel.send(byteBuffer, messageInfo);
			sctpChannel.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
/**
 * This method will send NACK message
 * @param host
 * @param port
 * @throws UnknownHostException
 * @throws IOException
 */
	public void sendnack(String host, int port) throws UnknownHostException,
			IOException {
		// System.out.println("1.2Client.sendnack()----from this"+getServer().getName()
		// + "-------{"+host+""+port);
		Message msg = new Message();
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		msg.type = 2;// request
		msg.message = "NACK Message";
		msg.setSender(getServer());
		sendMessage(host, port, byteBuffer, msg);
		// socket.close();
		// //System.out.println("1.2Client.sendnack()-----------}");
	}
/**
 * This method will update the neighbours list after getting ack in Spanning Tree algo.
 * @param host
 * @param port
 */
	public void updateNeighbors(String host, int port) {
		// System.out.print("Client.updateNeighbors() of "+getServer().getName()
		// + "-----{");
		// System.out.println(this.getRecvACKs()+":"+this.getSentACKs()+":"+this.getRecvNACKs()+":"+this.getSentNACKs());
		Host ngbr = new Host();
		ngbr.setName(host);
		ngbr.setPort(port);
		System.out.println("TREE NEIGHBOUR -->" + host);
		if (!this.isFirstBCOver)
			this.treeNeighbours.add(ngbr);
		// System.out.println("Client.updateNeighbors()-----}");

	}
/**
 * This method is to check all neighbours of a node are ready
 * @return
 * @throws UnknownHostException
 */
	public boolean isBranchesRdy() throws UnknownHostException {
		// System.out.println("Client.isBranchesRdy()---------{");

		int count = this.getRecvACKs() + this.getSentACKs()
				+ this.getRecvNACKs() + this.getSentNACKs() + this.getRcvReqs()
				+ this.getSentReqs();
		// System.out.println(this.getRecvACKs()+":"+this.getSentACKs()+":"+
		// this.getRecvNACKs()+":"+this.getSentNACKs()+":"+this.getRcvReqs()+":"+this.getSentReqs());
		currentHost.setName(InetAddress.getLocalHost().getHostName());
		currentHost.setPort(this.server.port);

		Host tempHost = hostmap.get(currentHost.getName() + "-"
				+ currentHost.getPort());
		int neighborsSize = tempHost.getNeighbours().size();
		if (neighborsSize > 0)
			// System.out.println(tempHost.getNeighbours());
			count = this.getRecvACKs() + this.getSentACKs();
		boolean res;
		if (isStartNode)
			res = (count == this.treeNeighbours.size())
					&& (count + this.getRecvNACKs() == this.getSentReqs());
		else
			res = (count == this.treeNeighbours.size() + 1);
		// System.out.println("Client.isBranchesRdy() is returning "+res);

		return res;// count must be equl to the size of neighbours list in
					// config

		// if isBranchesRdy is true we need to send tree branch ready to its
		// parent in tree
	}
/**
 * This method will read the configuration file
 * @throws UnknownHostException
 */
	public void readConfigFile() throws UnknownHostException {
		BufferedReader br = null;
		InetAddress ip = InetAddress.getLocalHost();
		String curr = ip.getLocalHost().toString();
		String domain = ".utdallas.edu";
		// domain="";// to be removed
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader("config.properties"));
			int numNodes = 0;
			boolean isNodeCnt = false;
			while ((sCurrentLine = br.readLine()) != null) {

				if (sCurrentLine.startsWith("#")
						|| "".equals(sCurrentLine.trim())) {
					continue;
				} else if (!isNodeCnt) {
					String[] spArr = sCurrentLine.replaceAll("\t\t", " ")
							.replaceAll("\t", " ").replaceAll(" ", ":")
							.split(":");
					if (spArr.length == 1) {
						numNodes = Integer.parseInt(spArr[0]);
						isNodeCnt = true;
					}

				}

				else {
					if (sCurrentLine.startsWith("#")
							|| "".equals(sCurrentLine.trim())) {
						continue;
					}
					for (int i = 1; i <= numNodes; i++) {

						String[] spArr = sCurrentLine.replaceAll("\t\t", " ")
								.replaceAll("\t", " ").replaceAll(" ", ":")
								.split(":");
						Host tempHost = new Host();
						tempHost.setId(i);
						tempHost.setName(spArr[0] + domain);
						tempHost.setPort(Integer.parseInt(spArr[1]));
						ArrayList<Integer> neighbours = new ArrayList<Integer>();
						for (int j = 2; j < spArr.length; j++) {
							neighbours.add(Integer.parseInt(spArr[j]));
						}
						tempHost.setNeighbours(neighbours);
						hostList.add(tempHost);
						hostmap.put(
								tempHost.getName() + "-" + tempHost.getPort(),
								tempHost);
						sCurrentLine = br.readLine();
					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	public void sendBCMsg(Message obj) throws UnknownHostException, IOException {

		// System.out.println("2.1:Client.sendBCMsg() from this"+getServer().getName()
		// + " ------{"+obj.getSender().getName()+""+obj.getSender().getPort());
		if (this.treePrnt != null
				&& !this.treeNeighbours.contains(this.treePrnt))
			this.treeNeighbours.add(this.treePrnt);
		if (this.treeNeighbours.size() == 1 && isBCstartnode()) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
			Message reqMsg = new Message();
			reqMsg.type = 4;// BROADCAST MESSAGE
			reqMsg.message = obj.getMessage();
			reqMsg.setSender(getServer());
			sendMessage(this.treePrnt.getName(), this.treePrnt.getPort(),
					byteBuffer, reqMsg);
		}
		if (this.treeNeighbours.size() > 1)
			for (Host h : this.treeNeighbours) {
				if (!h.getName().equals(obj.getSender().getName()))// send REQ
																	// messages
																	// apart
																	// from the
																	// sender of
																	// the REQ
																	// Message
				{
					ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
					Message reqMsg = new Message();
					reqMsg.type = 4;// BROADCAST MESSAGE
					reqMsg.message = obj.getMessage();
					reqMsg.setSender(getServer());
					sendMessage(h.getName(), h.getPort(), byteBuffer, reqMsg);
					// socket.close();
				}
			}
		else {
			// send BC ACK to its parent
			ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
			Message reqMsg = new Message();
			reqMsg.type = 5;// BROADCAST MESSAGE
			reqMsg.message = "BC DONE";
			reqMsg.setSender(getServer());
			sendMessage(obj.getSender().getName(), obj.getSender().getPort(),
					byteBuffer, reqMsg);
		}
	}

	public void sendBCDoneMsg(String host, int port)
			throws UnknownHostException, IOException {

		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		Message reqMsg = new Message();
		reqMsg.type = 5;// BROADCAST MESSAGE
		reqMsg.message = "BC DONE";
		reqMsg.setSender(getServer());
		sendMessage(getBcPrnt().getName(), getBcPrnt().getPort(), byteBuffer,
				reqMsg);
	}

	private byte[] convertToBytes(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(object);
		return bos.toByteArray();

	}

}
