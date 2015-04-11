/**
 * Created by Thomas Midson s4333060 on 19/03/15.
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.ArrayList;

public class NameServer {

    /**
     * Nameserver Implementation
     * Implements a non blocking name server for use by the client to get the port of a working server.
     * 
     * Accepts two types of messages, register queries and look up queries.
     * Register queries will register a server with the name server.  
     * The assumption that the servers will all have individual names will be implied
     * Look up queries will search the listings of the name server and return an IP address for the desired server.
     * 
     * Register query format 'REGISTER name IP port'
     * Lookup query format 'LOOKUP name'
     */ 

    public static int portNum;
    private Selector selector = null;
    private ServerSocketChannel serverSocketChannel = null;
    private ServerSocket serverSocket = null;

    /**
     * Hosts is Hashmap with type <String, String[]>
     * The key of the hashmap is the server name, the value for the hashmap
     * is a 2 
     */
    private HashMap hosts;

    /**
     * public int ParseMessage(String message)
     * Parses any message recieved by the name server.
     * Takes the messages and tokenizes it then anaylises the message based on token
     * 
     * Returns an array based on the outcome of the parsing.
     * If the message is a REGISTER query then the array will contain the name, address and port of the server to be registered.
     * If the message is an LOOKUP query then the array will contain the address of the server 
     * Otherwise the array will contain the appropriate error messages for the outcome of the message parsing
     */
    public ArrayList parseMessage(String message) {
		
	// Arraylist toe return the data to the client or server.
	ArrayList ret = new ArrayList<String>();
	// tokenized version of the message from the sender
	String[] tokens = message.toLowerCase().trim().split(" ");
	// System.out.println(tokens.length);
	 
	// Swtich statment which handles the queries from the sender, it the query
	// isn't a register or lookup query then they are returned an error
	switch (tokens[0]) {
	case "lookup":
	    
	    if (tokens.length != 2) {
		// System.out.println("lookup error");
		break;
	    }	

	    // check lookup query for the correct format and check the database for the entry
	    String query = tokens[1].toLowerCase().trim();
	    System.out.println(query);
	    // If the process is registered with the server then return the address and port for the sender
	    if (hosts.containsKey(query)) {
		// System.out.println("query in hashmap");
		String val[] = (String[])hosts.get(query);
		ret.add(val[0] + " " + val[1] + "\n");
		return ret;
		// if the process isn't registered then return the appropriate error
	    } else {
		ret.add(0, "Error: Process has not registered with the Name Server\n");
		return ret;
	    }
	    
	case "register":
	    if (tokens.length != 4) {
		// System.out.println("register error");
		ret.add("error");
		return ret;
	    } else {
		String name;
		String[] details = new String[2];
		name = tokens[1];
		details[0] = tokens[2];
		details[1] = tokens[3];
		// check if the port being provided is a reserved port
		if (Integer.parseInt(tokens[3]) < 1024) {
		    break;
		}
		hosts.put(name, details);
		
		// System.out.println(hosts.size());
		
		ret.add("registered\n");

		return ret;
	    }
	}
	// Default option is to return an error to the sender, this will close the connection
	ret.add("error");
	return ret;
    }

    // Nameserver constructor - Code based around the non-blocking TCP server provided.
    public NameServer() {
	hosts = new HashMap<String, String[]>();
	
	try {
	    // open selector
	    selector = Selector.open();
	    // open socket channel
	    serverSocketChannel = ServerSocketChannel.open();
	    // set the socket associated with this channel
	    serverSocket = serverSocketChannel.socket();
	    // set Blocking mode to non-blocking
	    serverSocketChannel.configureBlocking(false);
	    // bind portNum
	    try {
		serverSocket.bind(new InetSocketAddress(portNum));
	    } catch (Exception e) {
		System.err.println("Cannot listen on given port number " + portNum);
		System.exit(1);
	    }
	    // registers this channel with the given selector, returning a selection key
	    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	    // System.out.println("<TCPServer> Server is activated, listening on portNum: "+ portNum);
	    System.err.println("Name Server waiting for incoming connections ...");

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
				System.out.println(message);
				buffer = null;
			    }
			} finally {
			    if (buffer != null)
				buffer.clear();
			}
			// react by Client's message
			if (readBytes > 0) {
			    // System.out.println("Message from Client" + sc.getRemoteAddress() + ": " + message);
			    // check if the message is a lookup or register query
			    if (message.toLowerCase().trim().contains("exit")) {
				sc.close();
				break;
			    } else if (message.toLowerCase().trim().contains("lookup") || message.toLowerCase().trim().contains("register")) {
				// sc.register(key.selector(), SelectionKey.OP_WRITE, "parsing\n");
				ArrayList parsed = parseMessage(message);
				ByteBuffer send = Charset.forName("UTF-8").encode((String)parsed.get(0));
				sc.write(send);
				// System.out.println(sc.write(send));
				// sc.close();
				// buffer.flip();
				
				// if (parsed.get(0) == "error") {
				//     // System.out.println("crashed");

				//     // create bytebuffer and send it
				//     // ByteBuffer send = Charset.forName("UTF-8").encode((String)parsed.get(0));
				//     // System.out.println(send);
				//     // sc.register(selector, SelectionKey.OP_WRITE);
				//     // sc.write(send);
				//     // // sc.register(key.selector(), SelectionKey.OP_WRITE, parsed.get(0));
				//     sc.close();
				//     break;
				// } else {
				//     // parsing was successful, send the information back to the client
				//     ByteBuffer send = Charset.forName("UTF-8").encode((String)parsed.get(0));
				//     sc.register(selector, SelectionKey.OP_READ, buffer);
				//     System.out.println(parsed.get(0));
				//     sc.write(send);
				//     // sc.close();
				// }
			    } else {
				// junk message is received, close collection
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

    public static void main(String args[]) {
	// Ensure command line arguments are in the correct format
	if (args.length != 1) {
	    System.err.println("Invalid command line arguments for NameServer");
	    System.exit(1);
	}

	portNum = Integer.parseInt(args[0]);
	// System.out.println(portNum);
	// Start new nameserver
	new NameServer();
    }
    
}
