/**
 * Created by Thomas Midson (s4333060) on 19/03/15.
 */
import java.net.*;
import java.io.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.math.BigInteger;

public class Bank {

    private static int bankPort;
    private static int nameServerPort;
    
    /**
     * registers the bank server with the name server 
     */
    public void register() throws IOException {

	Socket sendSocket = null;
	PrintWriter out = null;
	BufferedReader in = null;

	try {
	    // Connect to the process listening on port 9000 on this host (localhost)
	    sendSocket = new Socket("127.0.0.1", nameServerPort);
	    out = new PrintWriter(sendSocket.getOutputStream(), true);
	    // "true" means flush at end of line
	    in = new BufferedReader(
		new InputStreamReader(sendSocket.getInputStream()));
	} catch (Exception e) {
	    e.printStackTrace();
	}

	String regString = "register bank localhost " + bankPort;
	// System.out.println(regString);
	out.println(regString);
	String ret = "";
	try {
	    ret = in.readLine();
	    // while (in.ready()) {
	    // 	ret = in.readLine();
	    // 	// ret = ret + (char)in.read();
	    // 	System.out.println(ret);
	    // }
	}
	catch (IOException e) {
	    System.out.println("Error " + e.getMessage());
	    e.printStackTrace();
	    System.err.println("Bank registration with NameServer failed");
	    System.exit(1);
	}
	if (!(ret.equalsIgnoreCase("registered"))) {
	    System.err.println("Bank registration with NameServer failed");
	    System.exit(1);
	}
	
	out.close();
	in.close();
	sendSocket.close();
    }

    public Bank() throws IOException {


	@SuppressWarnings("resource")
	    ServerSocket serverSocket = null;
	try {
	    serverSocket = new ServerSocket(bankPort);
	    register();
	} catch (IOException e) {
	    System.err.println("Bank unable to listen on given port");
	    System.exit(1);
	    // e.printStackTrace();
	}
	Socket connSocket = null;
	while(true) {
	    try {
		// block, waiting for a conn. request
		connSocket = serverSocket.accept();
		// At this point, we have a connection
		System.err.println("Bank waiting for incoming connections");
		// System.out.println("Connection accepted from: " + connSocket.getInetAddress().getHostName());
	    } catch (IOException e) {
		e.printStackTrace();
		System.out.println("Bank unable to listen on given port");
		System.exit(1);
	    }
	    // Now have a socket to use for communication
	    // Create a PrintWriter and BufferedReader for interaction with our stream "true" means we flush the stream on newline
	    PrintWriter out = new PrintWriter(connSocket.getOutputStream(), true);
	    BufferedReader in = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
	    String line;
	    // Read a line from the stream - until the stream closes
	    while ((line=in.readLine()) != null) {
		System.out.println("Message from client: " + line);
		// Client can close the connection by sending "exit"
		if(line.equalsIgnoreCase("exit")) {
		    break;
		}
		// Perform the job of the server - convert string to uppercase and return it

		Long itemId = new Long(line.trim().split(" ")[1]);

		if (itemId % 2 == 0) {
		    line = "1";
		} else {
		    line = "0";
		}
		System.out.println(line);
		// line = line.toUpperCase();
		out.println(line);
	    }
		    
	    System.out.println("Client " + connSocket.getInetAddress().getHostName() +" finish up");
	    out.close();
	    in.close();
	    connSocket.close();
	}

	
    }
    
    public static void main(String args[]) throws IOException {

	// Command line argument error checking
	if (args.length != 2) {
	    System.err.println("Invalid command line arguments for Bank");
	}

	bankPort = Integer.parseInt(args[0]);
	nameServerPort = Integer.parseInt(args[1]);


	new Bank();
    }
    
}
