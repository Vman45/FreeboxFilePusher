package eu.gaki.ffp.http;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import eu.gaki.ffp.FolderListener;
import eu.gaki.ffp.RssFileItem;

/**
 * The listener interface for receiving httpFolder events. The class that is
 * interested in processing a httpFolder event implements this interface, and
 * the object created with that class is registered with a component using the
 * component's <code>addHttpFolderListener<code> method. When
 * the httpFolder event occurs, that object's appropriate
 * method is invoked.
 *
 * @see HttpFolderEvent
 */
public class HttpFolderListener implements FolderListener {

	/** The http file server. */
	private HttpFileServer httpFileServer;

	/** The configuration. */
	private Properties configuration;

	/**
	 * Instantiates a new http folder listener.
	 *
	 * @param configuration
	 *            the configuration
	 */
	public HttpFolderListener(Properties configuration) {
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<RssFileItem> folderFile(Path dataFile, Path folder) throws IOException {
		
		
		return null;
	}

	/**
	 * Start http server.
	 *
	 * @throws NumberFormatException the number format exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private synchronized void startHttpServer() throws NumberFormatException, IOException {
		if (httpFileServer != null) {
			httpFileServer.start();
		} else {
			final String serverPort = configuration.getProperty("http.server.port", "80");
			final String serverIp = configuration.getProperty("http.server.ip", InetAddress.getLocalHost().getHostName());
			httpFileServer = new HttpFileServer(new InetSocketAddress(serverIp, Integer.valueOf(serverPort)));
		}
	}

	/**
	 * Stop http server.
	 */
	private synchronized void stopHttpServer() {
		if (httpFileServer != null) {
			httpFileServer.stop();
		}
	}
}
