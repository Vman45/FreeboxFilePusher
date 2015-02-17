package eu.gaki.ffp.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
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

	/** The files to serve. */
	private final Map<String, Path> filesToServe = new HashMap<>();
	
	/** The configuration. */
	private final Properties configuration;

	/**
	 * Instantiates a new http file server.
	 *
	 * @param address
	 *            the address
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException
	 */
	public HttpFileServer(Properties configuration, final InetSocketAddress address) throws IOException {
		this.configuration = configuration;
		this.address = address;
		this.connection = new SocketConnection(new ContainerSocketProcessor(new HttpFileServerService(this)));
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
	 * Adds the file to serve.
	 *
	 * @param path
	 *            the file to serve
	 * @return the file identifier
	 */
	public String addFileToServe(Path path) {
		String key = computeKey(path);
		filesToServe.put(key, path);
		return key;
	}

	/**
	 * Compute key.
	 *
	 * @param path
	 *            the path
	 * @return the string
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	private String computeKey(Path path) {
		String pathString = path.toAbsolutePath().toString();
		String key;
		try {
			key = DigestUtils.md5Hex(pathString.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Cannot compute the key for file: " + path, e);
			key = Integer.toString(pathString.hashCode());
		}
		return key;
	}

	/**
	 * Removes the file to serve.
	 *
	 * @param identifier
	 *            the identifier
	 * @return the path
	 */
	public Path removeFileToServe(Path path) {
		return filesToServe.remove(computeKey(path));
	}

	/**
	 * Gets the file to serve.
	 *
	 * @param identifier
	 *            the identifier
	 * @return the file to serve
	 */
	public Path getFileToServe(String identifier) {
		return filesToServe.get(identifier);
	}

	/**
	 * Checks if is file to serve empty.
	 *
	 * @return true, if is file to serve empty
	 */
	public boolean isFileToServeEmpty() {
		return filesToServe.isEmpty();
	}

	/**
	 * Gets the files to serve.
	 *
	 * @return the files to serve
	 */
	public Set<Map.Entry<String, Path>> getFilesToServe() {
		return new HashSet<java.util.Map.Entry<String, Path>>(filesToServe.entrySet());
	}

	/**
	 * Checks if is file served.
	 *
	 * @param path
	 *            the path
	 * @return true, if is file served
	 */
	public boolean isFileServed(Path path) {
		String key = computeKey(path);
		return filesToServe.containsKey(key);
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
	 * Removes the and delete file to serve.
	 *
	 * @param file
	 *            the file
	 */
	public void removeAndDeleteFileToServe(Path path) {
		removeFileToServe(path);
		try {
			final String deleteString = configuration.getProperty("delete.after.sending", "false");
			if (Boolean.valueOf(deleteString)) {
				Files.deleteIfExists(path);
			}
		} catch (IOException e) {
			LOGGER.error("Cannot delete the file.", e);
		}
	}

}
