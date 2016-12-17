import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

/**
 * Class <code>WebServer</code> -
 * Main class that starts the Web Server
 *
 * support method:
 * 		head
 * 		get
 * 	    options
 * 	    post
 *
 * support function:
 * 		file-directory-scan
 * 		persistent contention
 * 	  	conditional GET
 * 	  	simple php cgi
 */
public class WebServer extends Thread {

	private static Logger log = Logger.getLogger(WebServer.class);

	private static final int N_THREADS = 3;

	public static void main(String args[]) {
        Config config = new Config();

		try {
			new WebServer().start(config.getPort(), config.getConnectTimeout());
		} catch (Exception e) {
			log.error("Startup Error", e);
		}
	}

	public void start(int port, int socketTimeout) throws IOException {
		ServerSocket s = new ServerSocket(port);
		System.out.println("Web server listening on port " + port + " (press CTRL-C to quit)");
		ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
		while (true) {
			Socket client = s.accept();
			client.setSoTimeout(socketTimeout);
			executor.submit(new RequestHandler(client));
		}
	}
}
