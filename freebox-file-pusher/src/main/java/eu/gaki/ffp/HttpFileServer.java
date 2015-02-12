package eu.gaki.ffp;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HttpFileServer.
 */
public class HttpFileServer {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpFileServer.class);

    /** The connection. */
    private final SocketConnection connection;

    /** The server. */
    private Thread server;

    /** The address. */
    private final InetSocketAddress address;

    /**
     * Instantiates a new http file server.
     *
     * @param address
     *            the address
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public HttpFileServer(final InetSocketAddress address) throws IOException {
	this.address = address;
	this.connection = new SocketConnection(new ContainerSocketProcessor(new HttpFileServerService()));
    }

    /**
     * Start the tracker thread.
     */
    public void start() {
	if (this.server == null || !this.server.isAlive()) {
	    this.server = new ServerThread();
	    this.server.setName("HTTP file server: " + this.address.getPort());
	    this.server.start();
	}

    }

    /**
     * Stop.
     */
    public void stop() {
	try {
	    this.connection.close();
	    LOGGER.info("HTTP file server closed.");
	} catch (final IOException ioe) {
	    LOGGER.error("Could not stop the HTTP file server: {}!", ioe.getMessage());
	}

    }

    /**
     * The Class ServerThread.
     */
    private class ServerThread extends Thread {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
	    LOGGER.info("Starting HTTP file server on {}...", HttpFileServer.this.address);

	    try {
		connection.connect(address);
	    } catch (final IOException ioe) {
		LOGGER.error("Could not start the HTTP file server: {}!", ioe.getMessage());
		HttpFileServer.this.stop();
	    }
	}
    }

    /**
     * The main method.
     *
     * @param args
     *            the arguments
     * @throws NumberFormatException
     *             the number format exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void main(final String[] args) throws NumberFormatException, IOException {
	final HttpFileServer httpFileServer = new HttpFileServer(new InetSocketAddress("127.0.0.1",
		Integer.valueOf("80")));
	httpFileServer.start();
    }
}
