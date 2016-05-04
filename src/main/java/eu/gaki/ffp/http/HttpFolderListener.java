package eu.gaki.ffp.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.FolderListener;
import eu.gaki.ffp.RssFileItem;
import eu.gaki.ffp.compress.FilesCompresor;

/**
 * The listener interface for receiving httpFolder events. The class that is
 * interested in processing a httpFolder event implements this interface, and
 * the object created with that class is registered with a component using the
 * component's <code>addHttpFolderListener<code> method. When the httpFolder
 * event occurs, that object's appropriate method is invoked.
 *
 * @see HttpFolderEvent
 */
public class HttpFolderListener implements FolderListener {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpFolderListener.class);

    /** The http file server. */
    private final HttpFileServer httpFileServer;

    /** The configuration. */
    private final Properties configuration;

    /** The files compresor. */
    private final FilesCompresor filesCompresor;

    /**
     * Instantiates a new http folder listener.
     *
     * @param configuration
     *            the configuration
     * @throws IOException
     * @throws NumberFormatException
     */
    public HttpFolderListener(final Properties configuration) throws NumberFormatException, IOException {
	this.configuration = configuration;
	this.filesCompresor = new FilesCompresor(configuration);
	final String serverPort = configuration.getProperty("http.server.port", "80");
	final String serverIp = configuration.getProperty("http.server.ip", InetAddress.getLocalHost().getHostName());
	this.httpFileServer = new HttpFileServer(configuration,
		new InetSocketAddress(serverIp, Integer.valueOf(serverPort)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAlreadyPushed(final Path path) {
	return httpFileServer.isFileServed(path) || filesCompresor.isInProgress(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scanPath(final Path folderScanned, final Path path) throws IOException {
	if (Files.exists(path) && !Files.isDirectory(path) && !isAlreadyPushed(path)) {
	    LOGGER.debug("Found a file to serve: {}", path);
	    httpFileServer.addFileToServe(path);
	} else if (Files.isDirectory(path) && !isAlreadyPushed(filesCompresor.computeCompressFileName(path))) {
	    LOGGER.debug("Found a directory to compress{}", path);
	    filesCompresor.compress(path);
	}
    }

    /**
     * Compute rss item list.
     *
     * @param rssFileItems
     *            the rss file items
     */
    @Override
    public Set<RssFileItem> getRssItemList() {
	final Set<RssFileItem> rssFileItems = new HashSet<>();

	// Publish RSS file with served files
	final Map<String, Path> filesToServe = httpFileServer.getFilesToServe();
	final String httpUrlTemplate = configuration.getProperty("http.url", "http://unknown/${file.name}");
	filesToServe.entrySet().forEach(entry -> {
	    final Path entryPath = entry.getValue();
	    final RssFileItem rssFileItem = new RssFileItem();
	    // Rss link name
	    rssFileItem.setName(entryPath.getFileName().toString());
	    // Rss file URL
	    final String name = entry.getKey();
	    String nameUrl;
	    try {
		nameUrl = URLEncoder.encode(name, "UTF-8").replace("+", "%20");
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

	return rssFileItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterScans() {
	// Remove file which no more exist
	final Map<String, Path> filesToServe = httpFileServer.getFilesToServe();
	filesToServe.entrySet().forEach(entry -> {
	    final Path path = entry.getValue();
	    if (Files.notExists(path)) {
		httpFileServer.removeFileToServe(path);
		LOGGER.info("Remove http file: {}", path);
	    }
	});

	// If nothing to serve
	if (httpFileServer.isFileToServeEmpty()) {
	    httpFileServer.stop();
	} else {
	    httpFileServer.start();
	}
    }

}
