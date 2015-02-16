package eu.gaki.ffp.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpFolderListener.class);

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
	public void beginning(Path folderScanned) {
		// Remove tracked file which no more exist
		if (httpFileServer != null) {
			Set<Map.Entry<String, Path>> filesToServe = httpFileServer.getFilesToServe();
			filesToServe.forEach(entry -> {
				Path path = entry.getValue();
				if (Files.notExists(path)) {
					httpFileServer.removeFileToServe(path);
					LOGGER.info("Remove http file: {}", path);
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<RssFileItem> folderFile(Path folderScanned, Path path) throws IOException {
		final List<RssFileItem> rssFileItems = new ArrayList<>();

		startHttpServer();
		if (Files.exists(path) && !Files.isDirectory(path) && !httpFileServer.isFileServed(path)) {
			httpFileServer.addFileToServe(path);
		}

		if (httpFileServer != null) {
			// Publish RSS file with served files
			Set<Entry<String, Path>> filesToServe = httpFileServer.getFilesToServe();
			final String httpUrlTemplate = configuration.getProperty("http.url", "http://unknown/${file.name}");
			filesToServe.forEach(entry -> {
				Path entryPath = entry.getValue();
				RssFileItem rssFileItem = new RssFileItem();
				// Rss link name
					rssFileItem.setName(entryPath.getFileName().toString());
					// Rss file URL
					final String name = entry.getKey();
					String nameUrl;
					try {
						nameUrl = URLEncoder.encode(name, "UTF-8");
					} catch (final UnsupportedEncodingException e1) {
						LOGGER.error("Error when URL encode the file name. Fallback without URL encode", e1);
						nameUrl = name;
					}
					rssFileItem.setUrl(httpUrlTemplate.replace("${file.name}", nameUrl));
					// Rss file date
					try {
						final FileTime lastModifiedTime = Files.getLastModifiedTime(entryPath);
						rssFileItem.setDate(new Date(lastModifiedTime.toMillis()));
					} catch (IOException e) {
						LOGGER.error("Cannot determine the modification date of " + entryPath, e);
						rssFileItem.setDate(new Date());
					}
					// Rss file size
					try {
						rssFileItem.setSize(Files.size(entryPath));
					} catch (Exception e) {
						LOGGER.error("Cannot compute the size of "+entryPath,e);
						rssFileItem.setSize(0L);
					}
					rssFileItems.add(rssFileItem);
				});
		}

		return rssFileItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void ending(Path folderScanned) {
		if (httpFileServer != null && httpFileServer.isFileToServeEmpty()) {
			stopHttpServer();
		}
	}

	/**
	 * Start http server.
	 *
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private synchronized void startHttpServer() throws NumberFormatException, IOException {
		if (httpFileServer == null) {
			final String serverPort = configuration.getProperty("http.server.port", "80");
			final String serverIp = configuration.getProperty("http.server.ip", InetAddress.getLocalHost().getHostName());
			httpFileServer = new HttpFileServer(new InetSocketAddress(serverIp, Integer.valueOf(serverPort)));
			httpFileServer.start();
		}
	}

	/**
	 * Stop http server.
	 */
	private synchronized void stopHttpServer() {
		if (httpFileServer != null) {
			httpFileServer.stop();
			httpFileServer = null;
		}
	}
}
