package eu.gaki.ffp.bittorrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

import eu.gaki.ffp.FolderListener;
import eu.gaki.ffp.RssFileItem;

/**
 * The listener interface for receiving bittorrentFolder events. The class that
 * is interested in processing a bittorrentFolder event implements this
 * interface, and the object created with that class is registered with a
 * component using the component's
 * <code>addBittorrentFolderListener<code> method. When
 * the bittorrentFolder event occurs, that object's appropriate
 * method is invoked.
 *
 * @see BittorrentFolderEvent
 */
public class BittorrentFolderListener implements FolderListener {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(BittorrentFolderListener.class);

	/** The tracker. */
	private Tracker tracker;

	/** The configuration. */
	private final Properties configuration;

	/**
	 * Instantiates a new bittorrent folder listener.
	 *
	 * @param configuration
	 *            the configuration
	 */
	public BittorrentFolderListener(final Properties configuration) {
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAlreadyPushed(final Path path) {
		final Path torrentFile = computeTorrentFileName(path);
		return tracker.isTracked(torrentFile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void scanPath(final Path folderScanned, final Path path) throws IOException {
		// Share the file
		Path torrentFile = computeTorrentFileName(path);
		if (!Files.exists(torrentFile)) {
			// Create torrent file
			torrentFile = createTorrentFile(path);
		}
		startTracker();
		if (tracker != null && !tracker.isTracked(torrentFile)) {
			// Add file to tracker
			final TrackedTorrent torrent = TrackedTorrent.load(torrentFile.toFile());
			torrent.setSeederClient(new SeederClient(configuration, torrent, torrentFile, path));
			tracker.announce(torrent);
			LOGGER.info("Announce file: {}", torrentFile);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterScans() {
		if (tracker != null) {

			// Remove tracked file which no more exist
			final ArrayList<TrackedTorrent> trackedTorrents = new ArrayList<TrackedTorrent>(tracker.getTrackedTorrents());
			trackedTorrents.forEach(torrent -> {
				final SeederClient seederClient = torrent.getSeederClient();
				if (seederClient == null || seederClient.getTorrentFile() == null || Files.notExists(seederClient.getTorrentFile())) {
					tracker.remove(torrent);
					LOGGER.info("Remove announce file: {}", torrent.getName());
				}
			});

			// If no more tracker torrent stop the tracker
			if (tracker.getTrackedTorrents().size() == 0) {
				stopTracker();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<RssFileItem> getRssItemList() {
		final Set<RssFileItem> rssFileItems = new HashSet<>();
		// Publish RSS file with tracked torrent files
		final Collection<TrackedTorrent> trackedTorrents = tracker.getTrackedTorrents();
		final String torrentUrlTemplate = configuration.getProperty("torrent.url", "http://unknown/${file.name}");
		trackedTorrents.forEach(torrent -> {
			final RssFileItem rssFileItem = new RssFileItem();
			// Rss link name
			rssFileItem.setName(torrent.getName());
			// Rss file URL
			final String name = torrent.getName();
			String nameUrl;
			try {
				nameUrl = URLEncoder.encode(name, "UTF-8");
			} catch (final UnsupportedEncodingException e) {
				LOGGER.error("Error when URL encode the file name. Fallback without URL encode", e);
				nameUrl = name;
			}
			rssFileItem.setUrl(torrentUrlTemplate.replace("${file.name}", nameUrl + configuration.getProperty("torrent.extention", ".torrent")));
			// Rss file date
			final Path torrentFile = torrent.getSeederClient().getTorrentFile();
			FileTime lastModifiedTime;
			try {
				lastModifiedTime = Files.getLastModifiedTime(torrentFile);
				rssFileItem.setDate(new Date(lastModifiedTime.toMillis()));
			} catch (final Exception e) {
				LOGGER.error("Cannot determine the modification date of " + torrentFile, e);
				rssFileItem.setDate(new Date());
			}
			// Rss file size
			try {
				rssFileItem.setSize(Files.size(torrentFile));
			} catch (final Exception e) {
				LOGGER.error("Cannot compute the size of " + torrentFile, e);
				rssFileItem.setSize(0L);
			}
			rssFileItems.add(rssFileItem);
		});
		return rssFileItems;
	}

	/**
	 * Creates the torrent file.
	 *
	 * @param file
	 *            the file
	 * @return the file
	 */
	private Path createTorrentFile(final Path file) {
		Path torrentFile = null;
		torrentFile = computeTorrentFileName(file);

		try (OutputStream fos = new FileOutputStream(torrentFile.toFile())) {
			final String trackerAnnounceUrl = configuration.getProperty("tracker.announce.url", "http://unkown:6969/announce");

			final URI announceURI = new URI(trackerAnnounceUrl);

			final String creator = String.format("%s (FreeboxFilePusher)", System.getProperty("user.name"));

			Torrent torrent = null;
			if (Files.isDirectory(file)) {
				final File[] files = file.toFile().listFiles();
				Arrays.sort(files);
				torrent = Torrent.create(file.toFile(), Arrays.asList(files), announceURI, creator);
			} else {
				torrent = Torrent.create(file.toFile(), announceURI, creator);
			}
			torrent.save(fos);
			LOGGER.info("Create torrent file: {}", torrentFile);
		} catch (URISyntaxException | InterruptedException | IOException e) {
			LOGGER.error("Cannot create torrent file:" + e.getMessage(), e);
		}

		return torrentFile;
	}

	/**
	 * Compute torrent file name.
	 *
	 * @param file
	 *            the file
	 * @return the file
	 */
	private Path computeTorrentFileName(final Path file) {
		Path torrentFile;

		final String fileNameUrl = file.getFileName().toString();

		torrentFile = FileSystems.getDefault().getPath(configuration.getProperty("torrent.file.folder", "www-data"),
				fileNameUrl + configuration.getProperty("torrent.extention", ".torrent"));

		return torrentFile;
	}

	/**
	 * Start the tracker if not already started.
	 *
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private synchronized void startTracker() throws NumberFormatException, IOException {
		if (tracker == null) {
			final String trackerPort = configuration.getProperty("torrent.tracker.port", "6969");
			final String trackerIp = configuration.getProperty("torrent.tracker.ip", InetAddress.getLocalHost().getHostName());
			tracker = new Tracker(new InetSocketAddress(trackerIp, Integer.valueOf(trackerPort)), "FreeboxFilePusher");
			tracker.start();
			LOGGER.info("Tracker starded {}:{}", trackerIp, trackerPort);
		}
	}

	/**
	 * Stop the tracker if started.
	 */
	private synchronized void stopTracker() {
		if (tracker != null) {
			tracker.stop();
			tracker = null;
			LOGGER.info("Tracker stoped");
		}
	}

}
