/**
 * Created by Thomas Midson (s4333060) on 19/03/15.
 */
import java.net.*;
import java.io.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

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
	    System.err.println("Registration with NameServer failed");
	    System.exit(1);
	}
	if (!(ret.equalsIgnoreCase("registered"))) {
	    System.err.println("Registration with NameServer failed");
	    System.exit(1);
	}
	
	out.close();
	in.close();
	sendSocket.close();
    }

    public Bank() throws IOException {

	register();
	
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
