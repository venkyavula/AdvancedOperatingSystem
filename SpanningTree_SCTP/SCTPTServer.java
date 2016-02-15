import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

/**
 * This is the Server class of Broadcast Service. This has been implemented
 * using SCTP Protocol
 * 
 * @author vxa141230
 *
 */
public class SCTPTServer extends Thread {
	public static final int MESSAGE_SIZE = 1000;
	SctpChannel sctpChannel;
	SClient clientObj;
	int servPort = 0;

	public SCTPTServer(int port) {
		clientObj = new SClient();
		servPort = port;
	}

	@Override
	public void run() {
		try {
			go();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void go() throws IOException {

		SctpServerChannel sctpServerChannel = SctpServerChannel.open();
		InetSocketAddress serverAddr = new InetSocketAddress(servPort);
		sctpServerChannel.bind(serverAddr);

		Host currentHost = new Host();
		currentHost.setName(InetAddress.getLocalHost().getHostName());
		currentHost.setPort(servPort);

		clientObj.setServer(currentHost);
		clientObj.readConfigFile();
		System.out.println("Server : " + currentHost.getName()
				+ currentHost.getPort());
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		while (true) {
			sctpChannel = sctpServerChannel.accept();
			try {

				MessageInfo messageInfo = sctpChannel.receive(byteBuffer, null,
						null);
				byteBuffer.position(0);
				byteBuffer.limit(MESSAGE_SIZE);
				byte[] bytes = new byte[byteBuffer.remaining()];
				byteBuffer.get(bytes, 0, bytes.length);
				Message objRcvd = (Message) deserialize(bytes);
				// System.out.println("Connection--------"+objRcvd.getType());
				byteBuffer.clear(); // clear buffer contents to hold new
									// contents
				byteBuffer.put(new byte[MESSAGE_SIZE]);
				byteBuffer.clear();
				// System.out.println("Client : "+sctpChannel..getHostName() +
				// "req Type:"+objRcvd.getType());

				if ((objRcvd) != null) {
					// Req Has Come
					if (objRcvd.getType() == -1) {
						clientObj.isStartNode = true;
						clientObj.sendReq(objRcvd.getSender().getName(),
								objRcvd.getSender().getPort(), true);
					} else if (objRcvd.getType() == 0) {
						int c = clientObj.getRcvReqs();
						clientObj.setRcvReqs(++c);
						// Req Has Come
						clientObj.sendReq(objRcvd.getSender().getName(),
								objRcvd.getSender().getPort(), false);
					} else if (objRcvd.getType() == 1)// ACK
					{

						int curr = clientObj.getRecvACKs();
						clientObj.setRecvACKs(curr + 1);
						clientObj.updateNeighbors(
								objRcvd.getSender().getName(), objRcvd
										.getSender().getPort());

					} else if (objRcvd.getType() == 2)// NACK
					{
						int curr = clientObj.getRecvNACKs();
						clientObj.setRecvNACKs(curr + 1);
					} else if (objRcvd.getType() == -2) // BROADCAST MSG
					{
						clientObj.treeNeighbours = clientObj.treeBackup;
						clientObj.setBCstartnode(true);
						clientObj.rcvBCDone = 0;
						System.out.println("Sending a BC Message ::"
								+ objRcvd.getMessage());
						clientObj.sendBCMsg(objRcvd);

					} else if (objRcvd.getType() == 4) // BROADCAST MSG
					{
						clientObj.setBcPrnt(objRcvd.getSender());
						System.out.println("Got a BC Message ::"
								+ objRcvd.getMessage());
						clientObj.sendBCMsg(objRcvd);

					} else if (objRcvd.getType() == 5) // BROADCAST MSG DONE
					{
						clientObj.rcvBCDone++;
						if (clientObj.isBCstartnode) {
							if (clientObj.treeNeighbours.size() == clientObj
									.getRcvBCDone()) {
								System.out.println("BROADCAST OVER ");
								clientObj.isBCstartnode = false;
								clientObj.rcvBCDone = 0;
								clientObj.isFirstBCOver = true;
							}
						} else if (clientObj.treeNeighbours.size() - 1 == clientObj
								.getRcvBCDone()) {
							clientObj.sendBCDoneMsg(objRcvd.getSender()
									.getName(), objRcvd.getSender().getPort());
							clientObj.rcvBCDone = 0;
						}
					} else // TREE READY
					{
						int curr = clientObj.getRecvTRs();
						clientObj.setRecvTRs(curr + 1);
						clientObj.checkAndsendTreeReady();
					}
					// objRcvd = (Message)in.readObject();
				}
				sctpChannel.shutdown();
				sctpChannel.finishConnect();
				sctpChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out
						.println("Exception caught when trying to listen on port "
								+ " or listening for a connection ::");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static byte[] byteToString(ByteBuffer byteBuffer) {
		byteBuffer.position(0);
		byteBuffer.limit(MESSAGE_SIZE);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr, 0, bufArr.length);
		return bufArr;
	}

	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(obj);
		return b.toByteArray();
	}

	public static Object deserialize(byte[] bytes) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		ObjectInputStream o = new ObjectInputStream(b);
		return o.readObject();
	}

}
