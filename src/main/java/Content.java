/**
 * Created by tom on 19/03/15.
 */

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.*;

public class Content {
    
    private static String contentFile;
    private static int contentPort;
    private static int nameServerPort;
    private HashMap content;
    /**
     * registers the content server with the name server 
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

	String regString = "register content localhost " + contentPort;
	// System.out.println(regString);
	out.println(regString);
	String ret = "";
	try {
	    ret = in.readLine();
	}
	catch (IOException e) {
	    System.out.println("Error " + e.getMessage());
	    e.printStackTrace();
	    System.err.println("Content registration with NameServer failed");
	    System.exit(1);
	}
	if (!(ret.equalsIgnoreCase("registered"))) {
	    System.err.println("Content registration with NameServer failed");
	    System.exit(1);
	}
	
	out.close();
	in.close();
	sendSocket.close();
    }
    
    /**
     * public boolean readStockFile()
     * Reads the stock file given via the command line arguments and
     * parses it's content.
     *
     * Post: Populates the content hashmap

     */
    public boolean readStockFile() {
	Charset charset = Charset.forName("US-ASCII");
	try {
	    Path contentPath = Paths.get(contentFile);
	    BufferedReader buff =  Files.newBufferedReader(contentPath);
	    String line;
	    while ((line = buff.readLine()) != null) {
		String[] stockLine = line.split(" ");
		Long sNum = new Long(stockLine[0]);
		content.put(sNum, stockLine[1]);
	    }
	    for (Object id : content.keySet()) {
	    	System.out.println((Long)id + " " + content.get(id));
	    }
	} catch (IOException e) {
	    System.err.println("Invalid command line arguments for Content");
	    System.exit(1);
	    return false;
	}
	return true;
    }

    public Content() throws IOException {
	content = new HashMap<Long, String>();
	readStockFile();
	
	@SuppressWarnings("resource")
	    ServerSocket serverSocket = null;
	try {
	    serverSocket = new ServerSocket(contentPort);
	    register();
	} catch (IOException e) {
	    System.err.println("Content unable to listen on given port");
	    System.exit(1);
	    // e.printStackTrace();
	}
	Socket connSocket = null;
	while(true) {
	    try {
		// block, waiting for a conn. request
		System.err.println("Content waiting for incoming connections");
		connSocket = serverSocket.accept();
		// At this point, we have a connection
		// System.out.println("Connection accepted from: " + connSocket.getInetAddress().getHostName());
	    } catch (IOException e) {
		e.printStackTrace();
		System.out.println("Content unable to listen on given port");
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

		Long itemId = new Long(line);

		String contString = (String)content.get(itemId);
		
		System.out.println(itemId + " " + contString);
		
		// line = line.toUpperCase();
		out.println(contString);
	    }
		    
	    System.out.println("Client " + connSocket.getInetAddress().getHostName() +" finish up");
	    out.close();
	    in.close();
	    connSocket.close();
	}

    }


    public static void main(String args[]) {
	// Parse arguments
	if (args.length != 3) {
	    System.err.println("Invalid command line argument for Content");
	    System.exit(0);
	}
	// Assign arguments to globals
	contentPort = Integer.parseInt(args[0]);
	contentFile = args[1];
	nameServerPort = Integer.parseInt(args[2]);

	try {
	    // Start content server
	    new Content();

	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
}
