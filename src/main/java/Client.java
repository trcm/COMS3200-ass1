/**
 * Created by tom on 19/03/15.
 */
public class Client {

    public static void main(String args[]) {
	if (args.length != 1) {
	    System.err.println("Invalid command line arguments");
	    System.exit(0);
	}

	int request = Integer.parseInt(args[0]);
	if (request > 10 || request < 0) {
	    System.err.println("Invalid command line arguments");
	    System.exit(0);
	}
    }
    
}
