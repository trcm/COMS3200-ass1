/**
 * Created by tom on 19/03/15.
 */

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.math.BigInteger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;


class ServerStruct {
    private String name;
    private Socket serverSocket;
    private String host;
    private String port;
    public BufferedReader in;
    public PrintWriter out;

    public ServerStruct(String name, String host, String port) throws IOException {
	this.name = name;
	this.host = host;
	this.port = port;
	try {
	    serverSocket = new Socket(host, Integer.parseInt(port));
	} catch (ConnectException e) {
	    System.err.println("Unable to connect to " + this.name);
	    System.exit(1);
	}

	in = new BufferedReader(
	    new InputStreamReader(serverSocket.getInputStream()));
	out = new PrintWriter(serverSocket.getOutputStream(), true);
    }
}

public class Store {
    
    private Selector selector = null;
    private ServerSocketChannel serverSocketChannel = null;
    private ServerSocket serverSocket = null;
    //Port that the store server will listen on
    private static int stockPort;
    // Port for connecting to the nameserver
    private static int nameServerPort;
    // File containing content
    private static String stockFile;

    // Addresses for bank and content servers
    private String bankAdd;
    private String contentAdd;

    // HashMap holding the content numbers and their prices
    private HashMap content;

    // Sockets to connet to the bank and content servers
    // These strings are in the format "host port"
    private ServerStruct bankServer;
    private ServerStruct contentServer;
    
    /**
     * registers the store server with the name server 
     * and retrieves the addresses for the bank and content servers.
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
	    }
	    // for (Object id : content.keySet()) {
	    // 	System.out.println((BigInteger)id + " " + content.get(id));
	    // }
	} catch (IOException e) {
	    System.err.println("Invalid command line arguments for Store");
	    System.exit(1);
	    return false;
	}
	return true;
    }

    /**
     * Message the bank and return the banks response.  
     * The parameters taken by this function have already been checked for errors
     * by the parseMessage function.
     * @param itemId - BigInteger id of the file to be purchased
     * @param itemPrice - float representing the price of the item
     * @param ccNum - string representation of the users credit card number
     * @return If the bank accepts the transaction then return true, otherwise false.
     */
    public String messageBank(BigInteger itemId, Float itemPrice, String ccNum) {
	bankServer.out.println("purchase " + itemId.toString() + " " + itemPrice + " " + ccNum);
	String retString;
	try {
	    retString = bankServer.in.readLine();
	} catch (IOException e) {
	    retString = "Abort";
	}
	
	return retString;
    }
    
/**
 * parseMessage(String message)
 * 
 * Parses the messages recieved by the Store server. 
 * This server will only ever recieve messages for either looking up the 
 * stores contents or for purchasing content.
 *
 * @param message String representation of the message recieved
 * @return The parsed message that will be sent back to the client
 */
public String parseMessage(String message) {

    // Parse a lookup message, simply return the content of the store to the client
    if (message.toLowerCase().trim().equals("lookup")) {
	String ret = "";
	int count = 1;
	for (Object key : content.keySet()) {
	    ret = ret + count + ". " + (BigInteger)key + " " + content.get(key) + "\n";
	    count++;
	}
	return ret;
    } else if (message.toLowerCase().trim().contains("purchase")) {
	// Purchase request recieved. 

	//split the message to get the id number
	String idString = message.trim().split(" ")[1];
	String ccNum = message.trim().split(" ")[2];

	if (ccNum.length() != 16) {
	    return "Error";
	}
	    
	// Minus 1 to account for array index numbering
	int id = Integer.parseInt(idString) - 1;
	System.out.println(idString + "\n" + ccNum);
	if (id > 9 || id < 0) {
	    System.out.println("error");
	}

	// get the item id and item price from the stock hash
	Object[] ids = content.keySet().toArray();
	BigInteger itemId = (BigInteger)ids[id];
	Float itemPrice = (Float)content.get(itemId);

	// itemId, itemPrice and ccNum have been found, contact the bank

	String outcome = messageBank(itemId, itemPrice, ccNum);

	if (outcome.trim().equals("1")) {
	    // message content server
	    contentServer.out.println(itemId.toString());
	    String contentRet;

	    try {
		contentRet = contentServer.in.readLine();
		// if the content server returned the correct response, send it to the user
		if (contentRet.length() > 1) {
		    return contentRet;
		}
	    } catch (IOException e) {
		// something went wrong
	    }

	    //otherwise send the aborted message
	    String aborted = (id + 1) + " transaction aborted\n";
	    return aborted;
	} else {
	    // something went wrong, send the transaction aborted mesasge
	    // add one to get back to the original id number
	    String aborted = (id + 1) + " transaction aborted\n";
	    return aborted;
	}
	    
    } else {
	// Just request recieved.
	// TODO junk request
    }
	
    return "";
}
    
/**
 *  connectToServer()
 *  Opens the connections to the bank and content servers.
     
 * Alters the bankSocket and contentSocket variables.
 */
    public boolean connectToServers() throws IOException {
	// TODO Refactor to remove magic numbers
	// connect to the bank
	String[][] serverAdds = new String[2][2];
	// split the strings into [host, port] 
	serverAdds[0] = bankAdd.split(" ");
	serverAdds[1] = contentAdd.split(" ");
	bankServer = new ServerStruct("Bank", serverAdds[0][0], serverAdds[0][1]);
	// bankServer.out.println("bank");
	contentServer = new ServerStruct("Content", serverAdds[1][0], serverAdds[1][1]);
	// contentServer.out.println("content");
	
	return true;
    }
    
    public Store() throws IOException {
	
	content = new HashMap<BigInteger, Float>();

	register();
	connectToServers();
	if (!(readStockFile())) {
	    System.exit(1);
	}
	
	
	// ServerSocket serversocket = null;
	// try {
	//     // listen on port 9000
	//     serversocket = new ServerSocket(stockPort);
	//     System.err.println("Store waiting for incoming connections");
	// } catch (IOException e) {
	//     // e.printStackTrace();
	//     System.err.println("Store unable to list on given port");
	//     System.exit(1);
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
	
    
	try {
	    // open selector
	    selector = Selector.open();
	    // open socket channel
	    serverSocketChannel = ServerSocketChannel.open();
	    // set the socket associated with this channel
	    serverSocket = serverSocketChannel.socket();
	    // set Blocking mode to non-blocking
	    serverSocketChannel.configureBlocking(false);
	    // bind port
	    serverSocket.bind(new InetSocketAddress(stockPort));
	    // registers this channel with the given selector, returning a selection key
	    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	    System.err.println("Store waiting for incoming connections");

	    while (selector.select() > 0) {
		for (SelectionKey key : selector.selectedKeys()) {
		    // test whether this key's channel is ready to accept a new socket connection
		    if (key.isAcceptable()) {
			// accept the connection
			ServerSocketChannel server = (ServerSocketChannel) key.channel();
			SocketChannel sc = server.accept();
			if (sc == null)
			    continue;
			// System.out.println("Connection accepted from: " + sc.getRemoteAddress());
			// set blocking mode of the channel
			sc.configureBlocking(false);
			// allocate buffer
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			// set register status to READ
			sc.register(selector, SelectionKey.OP_READ, buffer);
		    }
		    // test whether this key's channel is ready for reading from Client
		    else if (key.isReadable()) {
			// get allocated buffer with size 1024
			ByteBuffer buffer = (ByteBuffer) key.attachment();
			SocketChannel sc = (SocketChannel) key.channel();
			int readBytes = 0;
			String message = null;
			// try to read bytes from the channel into the buffer
			try {
			    int ret;
			    try {
				while ((ret = sc.read(buffer)) > 0)
				    readBytes += ret;
			    } catch (Exception e) {
				readBytes = 0;
			    } finally {
				buffer.flip();
			    }
			    // finished reading, form message
			    if (readBytes > 0) {
				message = Charset.forName("UTF-8").decode(buffer).toString();
				buffer = null;
			    }
			} finally {
			    if (buffer != null)
				buffer.clear();
			}
			// react by Client's message
			if (readBytes > 0) {

			    // parse message
			    String response = parseMessage(message);

			    System.out.println("Message from Client" + sc.getRemoteAddress() + ": " + message);
			    // if exit, close socket channel
			    if ("exit".equalsIgnoreCase(message.trim())) {
				System.out.println("Client " + sc.getRemoteAddress() +" finish up");
				sc.close();
			    } else {
				// set register status to WRITE
				// sc.register(key.selector(), SelectionKey.OP_WRITE, message.toUpperCase());
				
				sc.write(Charset.forName("UTF-8").encode(response));
				System.out.println("Client" + sc.getRemoteAddress() + " Response:\n" + response);
				sc.close();
			    }
			}
		    }
		    // test whether this key's channel is ready for sending to Client
		    else if (key.isWritable()) {
			SocketChannel sc = (SocketChannel) key.channel();
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			buffer.put(((String) key.attachment()).getBytes());
			buffer.flip();
			sc.write(buffer);
			// set register status to READ
			sc.register(key.selector(), SelectionKey.OP_READ, buffer);
		    }
		}
		if (selector.isOpen()) {
		    selector.selectedKeys().clear();
		} else {
		    break;
		}
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (serverSocketChannel != null) {
		try {
		    serverSocketChannel.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
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
