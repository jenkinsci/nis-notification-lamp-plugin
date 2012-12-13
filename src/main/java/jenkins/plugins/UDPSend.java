package jenkins.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPSend {
	
	private static final Logger LOG = Logger.getLogger(UDPSend.class.getName());
	private InetAddress 		server;
	private int 				port;
	private DatagramSocket	    socket;
	private String				serverIdent;
	protected static final int  SO_TIMEOUT = 200000;

	/**
	 * Send a Msg via UDP
	 * 
	 * @param String buildName
	 * @param String msg
	 * @param server
	 * @param port
	 * @throws IOException
	 */
	public void sendResult(String buildName, String msg, InetAddress server, int port) throws IOException {
		DatagramSocket s = new DatagramSocket();
		s.setSoTimeout(SO_TIMEOUT);
		s.connect(server,port);
		this.socket = s;
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("NAME"+ " "+ buildName);
		pw.println("RESULT"+" " + msg);
		pw.println("QUIT");
		pw.flush();
		try {
			this.send(sw.toString());
		} catch (IOException iox) {
			LOG.log(Level.SEVERE, "Unable to send result to "+serverIdent, iox);
		}
		this.socket.close();
		this.socket = null;
	}
	
	/**
	 * Send Packet
	 * @param String msg
	 * @throws IOException
	 */
	private void send(String msg) throws IOException {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.server, this.port);
        this.socket.send(packet);
	}
}