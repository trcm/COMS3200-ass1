/**
 * Created by tom on 19/03/15.
 */

import java.net.*;
import java.io.*;

public class Client {

    public String storeAddress;
    
    public String getStoreAddress(int port) {
	Socket sendSocket = null;
	PrintWriter out = null;
	BufferedReader in = null;

	try {
	    sendSocket = new Socket("127.0.0.1", port);
	    out = new PrintWriter(sendSocket.getOutputStream(), true);
	    in = new BufferedReader(
		new InputStreamReader(sendSocket.getInputStream()));
	} catch (Exception e) {
	    System.err.println("Client unable to connect with NameServer");
	    System.exit(1);
	}

	String query = "lookup store";

	out.println(query);

	String address = "";
	try {
	    address = in.readLine();
	    if (address.trim().contains("Error")) {
		System.err.println("Client unable to connect to Store");
		System.exit(1);
	    }
	} catch (IOException e) {
	    System.err.println("Client unable to connect with NameServer");
	    System.exit(1);
	}
	return address;
    }

    public Client(int request, int nameServerPort) {
	storeAddress = getStoreAddress(nameServerPort);
	// connect to store and send request
	
	Socket sendSocket = null;
	PrintWriter out = null;
	BufferedReader in = null;

	String hostname = storeAddress.trim().split(" ")[0];
	int port = Integer.parseInt(storeAddress.trim().split(" ")[1]);
	try {
	    sendSocket = new Socket(hostname, port);
	    out = new PrintWriter(sendSocket.getOutputStream(), true);
	    in = new BufferedReader(
		new InputStreamReader(sendSocket.getInputStream()));
	} catch (Exception e) {
	    System.err.println("Client unable to connect with NameServer");
	    System.exit(1);
	}

	String query = "";
	if (request == 0) {
	    // send look up query
	    query = "lookup";
	} else {
	    // send purchase query with fake credit card number
	    query = "purchase " + request + " 4321432143214321";
	    // System.out.println(query);
	}
	
	out.println(query);

	String response = "";
	try {
	    while ((response = in.readLine()) != null) {
		System.out.println(response);
	    }
	} catch (IOException e) {
	    System.err.println("Client unable to connect with Store");
	    System.exit(1);
	}
	
    }
    
    public static void main(String args[]) {
	if (args.length != 2) {
	    System.err.println("Invalid command line arguments");
	    System.exit(0);
	}
	int request  = 0;
	int nameServerPort = 0; 
	try {
	    request = Integer.parseInt(args[0]);
	    nameServerPort = Integer.parseInt(args[1]);
	} catch (Exception e) {
	    System.err.println("Invalid command line arguments");
	    System.exit(1);
	}
	if (request > 10 || request < 0) {
	    System.err.println("Invalid command line arguments");
	    System.exit(0);
	}
	
	// send messages to the store server
	// if (request == 0) {
	//     // look up request, send the request to the store

	// } else {
	//     // purchase request
	    
	// }
	Client client = new Client(request, nameServerPort);
    }
    
}
