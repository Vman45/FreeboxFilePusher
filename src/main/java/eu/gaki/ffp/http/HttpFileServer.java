package eu.gaki.ffp.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.codec.digest.DigestUtils;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.SocketProcessor;
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
	private SocketConnection connection;

	/** The address. */
	private final InetSocketAddress address;

	/** The files to serve. */
	private final Map<String, Path> filesToServe = new HashMap<>();

	/** The configuration. */
	private final Properties configuration;

	/** The http file server executor. */
	Executor httpFileServerExecutor = Executors.newSingleThreadExecutor();

	private final HttpFileServerService httpFileServerService;

	/**
	 * Instantiates a new http file server.
	 *
	 * @param address
	 *            the address
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException
	 */
	public HttpFileServer(final Properties configuration, final InetSocketAddress address) throws IOException {
		this.configuration = configuration;
		this.address = address;
		httpFileServerService = new HttpFileServerService(configuration, this);
	}

	/**
	 * Start the tracker thread.
	 */
	public void start() {
		if (connection == null) {
			httpFileServerExecutor.execute(() -> {
				LOGGER.info("Starting HTTP file server on {}...", address);
				try {
					final SocketProcessor processor = new ContainerSocketProcessor(httpFileServerService);
					connection = new SocketConnection(processor);
					connection.connect(address);
				} catch (final IOException ioe) {
					LOGGER.error("Could not start the HTTP file server: {}!", ioe.getMessage(), ioe);
					HttpFileServer.this.stop();
				}
				LOGGER.info("HTTP file server start: {}", address);
			});
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		if (connection != null) {
			try {
				connection.close();
				connection = null;
				// Garbage all connection object after server end
				httpFileServerExecutor.execute(() -> {
					System.gc();
				});
				LOGGER.info("HTTP file server closed: {}", this.address);
			} catch (final IOException ioe) {
				LOGGER.error("Could not stop the HTTP file server: {}!", ioe.getMessage(), ioe);
			}
		}
	}

	/**
	 * Adds the file to serve.
	 *
	 * @param path
	 *            the file to serve
	 * @return the file identifier
	 */
	public String addFileToServe(final Path path) {
		LOGGER.trace("Start serving file {}", path);
		final String key = computeKey(path);
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
	private String computeKey(final Path path) {
		final String pathString = path.toAbsolutePath().toString();
		String key;
		try {
			key = DigestUtils.md5Hex(pathString.getBytes("UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			LOGGER.error("Cannot compute the key for file: {}", path, e);
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
	public Path removeFileToServe(final Path path) {
		LOGGER.trace("Stop serving file {}", path);
		return filesToServe.remove(computeKey(path));
	}

	/**
	 * Gets the file to serve.
	 *
	 * @param identifier
	 *            the identifier
	 * @return the file to serve
	 */
	public Path getFileToServe(final String identifier) {
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
	public Map<String, Path> getFilesToServe() {
		return new HashMap<>(filesToServe);
	}

	/**
	 * Checks if is file served.
	 *
	 * @param path
	 *            the path
	 * @return true, if is file served
	 */
	public boolean isFileServed(final Path path) {
		final String key = computeKey(path);
		return filesToServe.containsKey(key);
	}

	/**
	 * Removes the and delete file to serve.
	 *
	 * @param file
	 *            the file
	 */
	public void removeAndDeleteFileToServe(final Path path) {
		removeFileToServe(path);
		try {
			final String deleteString = configuration.getProperty("delete.after.sending", "false");
			if (Boolean.valueOf(deleteString)) {
				LOGGER.info("Delete file {}", path);
				Files.deleteIfExists(path);
			}
		} catch (final IOException e) {
			LOGGER.error("Cannot delete the file.", e);
		}
	}

}