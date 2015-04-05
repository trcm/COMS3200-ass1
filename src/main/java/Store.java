/**
 * Created by tom on 19/03/15.
 */

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.math.BigInteger;

public class Store {

    //Port that the store server will listen on
    public static int stockPort;
    // Port for connecting to the nameserver
    public static int nameServerPort;
    // File containing content
    public static String stockFile;

    // Addresses for bank and content servers
    public static String bankAdd;
    public static String contentAdd;

    // HashMap holding the content numbers and their prices
    private HashMap content;
    
    /**
     * registers the store server with the name server 
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

	String regString = "register store localhost " + stockPort;
	System.out.println(regString);
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
	}
	if (!(ret.equalsIgnoreCase("registered"))) {
	    System.err.println("Registration with NameServer failed");
	    System.exit(1);
	}


	// try and get the bank and content server addresses
	out.println("lookup bank");
	bankAdd = in.readLine();

	if (bankAdd.contains("Error")) {
	    System.err.println("Bank has not registered");
	    System.exit(1);
	}

	out.println("lookup content");
	contentAdd = in.readLine();

	if (contentAdd.contains("Error")) {
	    System.err.println("Content has not registered");
	    System.exit(1);
	}
	
	System.out.println(bankAdd);
	System.out.println(contentAdd);
	
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
	    Path stock = Paths.get(stockFile);
	    BufferedReader buff =  Files.newBufferedReader(stock);
	    String line;
	    while ((line = buff.readLine()) != null) {
		String[] stockLine = line.split(" ");
		BigInteger sNum = new BigInteger(stockLine[0]);
		content.put(sNum, Float.parseFloat(stockLine[1]));
		// System.out.println(line);
	    }

	} catch (IOException e) {
	    return false;
	}
	return true;
    }

    
    public Store() throws IOException {
	
	content = new HashMap<BigInteger, Float>();

	register();
	System.out.println(readStockFile());
	System.out.println(content.size());
	
	
	// ServerSocket serversocket = null;
	// try {
	//     // listen on port 9000
	//     serversocket = new ServerSocket(stockPort);
	//     System.out.println("<tcpserver> server is activated, listening on port: 9000");
	// } catch (IOException e) {
	//     e.printStackTrace();
	// }
	// Socket connsocket = null;
	// while(true) {
	//     try {
	// 	// block, waiting for a conn. request
	// 	connsocket = serversocket.accept();
	// 	// at this point, we have a connection
	// 	System.out.println("connection accepted from: " + connsocket.getInetAddress().getHostName());
	//     } catch (IOException e) {
	// 	e.printStackTrace();
	//     }
	//     // now have a socket to use for communication
	//     // create a printwriter and bufferedreader for interaction with our stream "true" means we flush the stream on newline
	//     PrintWriter out = new PrintWriter(connsocket.getOutputStream(), true);
	//     BufferedReader in = new BufferedReader(new InputStreamReader(connsocket.getInputStream()));
	//     String line;
	//     // read a line from the stream - until the stream closes
	//     while ((line=in.readLine()) != null) {
	// 	System.out.println("message from client: " + line);
	// 	// client can close the connection by sending "exit"
	// 	if(line.equalsIgnoreCase("exit")) {
	// 	    break;
	// 	}
	// 	// perform the job of the server - convert string to uppercase and return it
	// 	line = line.toUpperCase();
	// 	out.println(line);
	//     }
		    
	//     System.out.println("client " + connsocket.getInetAddress().getHostName() +" finish up");
	//     out.close();
	//     in.close();
	//     connsocket.close();
	// }
	
    } 

    public static void main(String[] args) throws IOException {

	if (args.length != 3) {
	    System.err.println("Invalid command line arguments for Store");
	    System.exit(1);
	}

	stockPort = Integer.parseInt(args[0]);
	stockFile = args[1];
	nameServerPort = Integer.parseInt(args[2]);

	// start up the store server
	new Store();
    }
    
}
