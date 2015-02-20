package eu.gaki.ffp.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.FolderListener;
import eu.gaki.ffp.RssFileItem;
import eu.gaki.ffp.compress.FilesCompresor;

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
    private final Properties configuration;

    /** The files compresor. */
    private final FilesCompresor filesCompresor;

    /**
     * Instantiates a new http folder listener.
     *
     * @param configuration
     *            the configuration
     */
    public HttpFolderListener(final Properties configuration) {
	this.configuration = configuration;
	this.filesCompresor = new FilesCompresor(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginning(final Path folderScanned) {
	// Remove tracked file which no more exist
	if (httpFileServer != null) {
	    final Set<Map.Entry<String, Path>> filesToServe = httpFileServer.getFilesToServe();
	    filesToServe.forEach(entry -> {
		final Path path = entry.getValue();
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
    public Collection<RssFileItem> folderFile(final Path folderScanned, final Path path) throws IOException {
	final Set<RssFileItem> rssFileItems = new HashSet<>();

	startHttpServer();
	if (Files.exists(path) && !Files.isDirectory(path) && !httpFileServer.isFileServed(path)) {
	    httpFileServer.addFileToServe(path);
	} else if (Files.isDirectory(path)) {
	    final String property = configuration.getProperty("ffp.compress.folder", "true");
	    if (Boolean.valueOf(property)) {
		filesCompresor.compress(path);
		FileUtils.deleteDirectory(path.toFile());
	    }
	}

	computeRssItemList(rssFileItems);

	return rssFileItems;
    }

    /**
     * Compute rss item list.
     *
     * @param rssFileItems
     *            the rss file items
     */
    private void computeRssItemList(final Set<RssFileItem> rssFileItems) {
	if (httpFileServer != null) {
	    // Publish RSS file with served files
	    final Set<Entry<String, Path>> filesToServe = httpFileServer.getFilesToServe();
	    final String httpUrlTemplate = configuration.getProperty("http.url", "http://unknown/${file.name}");
	    filesToServe.forEach(entry -> {
		final Path entryPath = entry.getValue();
		final RssFileItem rssFileItem = new RssFileItem();
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
		} catch (final IOException e) {
		    LOGGER.error("Cannot determine the modification date of {}", entryPath, e);
		    rssFileItem.setDate(new Date());
		}
		// Rss file size
		try {
		    rssFileItem.setSize(Files.size(entryPath));
		} catch (final Exception e) {
		    LOGGER.error("Cannot compute the size of {}", entryPath, e);
		    rssFileItem.setSize(0L);
		}
		rssFileItems.add(rssFileItem);
	    });
	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ending(final Path folderScanned) {
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
	    httpFileServer = new HttpFileServer(configuration, new InetSocketAddress(serverIp, Integer.valueOf(serverPort)));
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
