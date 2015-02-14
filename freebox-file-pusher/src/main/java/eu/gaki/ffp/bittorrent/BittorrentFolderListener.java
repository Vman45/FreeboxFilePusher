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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

import eu.gaki.ffp.FolderListener;
import eu.gaki.ffp.RssFileGenerator;
import eu.gaki.ffp.RssFileItem;

// TODO: Auto-generated Javadoc
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
	private Properties configuration;

	/**
	 * Instantiates a new bittorrent folder listener.
	 *
	 * @param configuration
	 *            the configuration
	 */
	public BittorrentFolderListener(Properties configuration) {
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<RssFileItem> folderFile(Path dataFile, Path folder) throws IOException {
		List<RssFileItem> rssFileItems = null;
		
		boolean modifyAnnounceList = false;

		modifyAnnounceList = shareByTorrent(dataFile);

		if (tracker != null) {
			// Remove announce of deleted torrent
			for (final TrackedTorrent torrent : new ArrayList<TrackedTorrent>(tracker.getTrackedTorrents())) {
				final SeederClient seederClient = torrent.getSeederClient();
				if (seederClient == null || seederClient.getTorrentFile() == null || !seederClient.getTorrentFile().isFile()) {
					tracker.remove(torrent);
					modifyAnnounceList = true;
					LOGGER.info("Remove announce file: {}", torrent.getName());
				}
			}
			if (modifyAnnounceList) {
				// Publish RSS file with tracked torrent files
				Collection<TrackedTorrent> trackedTorrents = tracker.getTrackedTorrents();
				rssFileItems = new ArrayList<>();
				final String torrentUrlTemplate = configuration.getProperty("torrent.url", "http://unknown/${file.name}");
				for (TrackedTorrent torrent : trackedTorrents) {
					RssFileItem rssFileItem = new RssFileItem();
					// Rss link name
					rssFileItem.setName(torrent.getName());
					// Rss torrent URL 
					final String name = torrent.getName();
					String nameUrl;
					try {
						nameUrl = URLEncoder.encode(name, "UTF-8");
					} catch (final UnsupportedEncodingException e) {
						LOGGER.error("Error when URL encode the file name. Fallback without URL encode", e);
						nameUrl = name;
					}
					rssFileItem.setUrl(torrentUrlTemplate.replace("${file.name}", nameUrl + configuration.getProperty("torrent.extention", ".torrent")));
					// Rss torrent date
					final File torrentFile = torrent.getSeederClient().getTorrentFile();
					final FileTime lastModifiedTime = Files.getLastModifiedTime(torrentFile.toPath());
					rssFileItem.setDate(new Date(lastModifiedTime.toMillis()));
				}
			}
			// If no more tracker torrent stop the tracker
			if (tracker.getTrackedTorrents().size() == 0) {
				stopTracker();
			}
		}
		return rssFileItems;
	}

	/**
	 * Share by torrent.
	 *
	 * @param dataFile
	 *            the data file
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean shareByTorrent(final Path dataFile) throws IOException {
		boolean modifyTrackerAnnounceList = false;
		File torrentFile = computeTorrentFileName(dataFile);
		if (!torrentFile.exists()) {
			// Create torrent file
			torrentFile = createTorrentFile(dataFile);
		}
		startTracker();
		if (tracker != null && !tracker.isTracked(torrentFile)) {
			// Add file to tracker
			final TrackedTorrent torrent = TrackedTorrent.load(torrentFile);
			torrent.setSeederClient(new SeederClient(configuration, torrent, torrentFile, dataFile));
			tracker.announce(torrent);
			modifyTrackerAnnounceList = true;
			LOGGER.info("Announce file: {}", torrentFile.getPath());
		}
		return modifyTrackerAnnounceList;
	}

	/**
	 * Creates the torrent file.
	 *
	 * @param file
	 *            the file
	 * @return the file
	 */
	private File createTorrentFile(final Path file) {
		File torrentFile = null;
		torrentFile = computeTorrentFileName(file);

		try (OutputStream fos = new FileOutputStream(torrentFile)) {
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
			LOGGER.info("Create torrent file: {}", torrentFile.getPath());
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
	private File computeTorrentFileName(final Path file) {
		File torrentFile;

		final String fileNameUrl = file.getFileName().toString();

		torrentFile = new File(configuration.getProperty("torrent.file.folder", "www-data"), fileNameUrl
				+ configuration.getProperty("torrent.extention", ".torrent"));
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
