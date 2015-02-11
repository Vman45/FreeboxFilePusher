package eu.gaki.ffp;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

/**
 * The Class FreeboxFilePusherRunnable.
 */
public class FreeboxFilePusherRunnable implements Runnable {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FreeboxFilePusherRunnable.class);

    /** The configuration. */
    private final Properties configuration;

    /** The torrent rss. */
    private final TorrentRss torrentRss = new TorrentRss();

    /** The tracker. */
    private Tracker tracker;

    /**
     * Instantiates a new freebox file pusher runnable.
     *
     * @param configuration
     *            the configuration
     * @param tracker
     *            the tracker
     */
    public FreeboxFilePusherRunnable(final Properties configuration) {
	this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
	try {
	    boolean modifyTrackerAnnounceList = false;

	    // Watch new files and folder in watched folder
	    final String folderLocation = configuration.getProperty(
		    "folders.to.watch", null);
	    if (folderLocation != null) {
		final File folder = new File(folderLocation);
		if (folder.isDirectory()) {
		    final File[] list = folder.listFiles();
		    for (final File dataFile : list) {
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
		    }
		}
	    }

	    if (tracker != null) {
		// Remove announce of deleted torrent
		for (final TrackedTorrent torrent : new ArrayList<TrackedTorrent>(tracker.getTrackedTorrents())) {
		    final SeederClient seederClient = torrent.getSeederClient();
		    if (seederClient == null || seederClient.getTorrentFile() == null
			    || !seederClient.getTorrentFile().isFile()) {
			tracker.remove(torrent);
			modifyTrackerAnnounceList = true;
			LOGGER.info("Remove announce file: {}", torrent.getName());
		    }
		}
		if (modifyTrackerAnnounceList) {
		    // Publish RSS file with tracked torrent files
		    torrentRss.generateRss(configuration, tracker.getTrackedTorrents());
		}
		// If no more tracker torrent stop the tracker
		if (tracker.getTrackedTorrents().size() == 0) {
		    stopTracker();
		}
	    }

	} catch (final Exception e) {
	    LOGGER.error("Cannot watch folder:" + e.getMessage(), e);
	}
    }

    /**
     * Creates the torrent file.
     *
     * @param file
     *            the file
     * @return the file
     */
    private File createTorrentFile(final File file) {
	File torrentFile = null;
	torrentFile = computeTorrentFileName(file);

	try (OutputStream fos = new FileOutputStream(torrentFile)) {
	    final String trackerAnnounceUrl = configuration.getProperty("tracker.announce.url",
		    "http://unkown:6969/announce");

	    final URI announceURI = new URI(trackerAnnounceUrl);

	    final String creator = String.format("%s (FreeboxFilePusher)",
		    System.getProperty("user.name"));

	    Torrent torrent = null;
	    if (file.isDirectory()) {
		final File[] files = file.listFiles();
		Arrays.sort(files);
		torrent = Torrent.create(file, Arrays.asList(files),
			announceURI, creator);
	    } else {
		torrent = Torrent.create(file, announceURI, creator);
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
    private File computeTorrentFileName(final File file) {
	File torrentFile;

	String fileNameUrl;
	try {
	    fileNameUrl = URLEncoder.encode(file.getName(), "UTF-8");
	} catch (final UnsupportedEncodingException e) {
	    LOGGER.error("Error when URL encode the file name. Fallback without URL encode", e);
	    fileNameUrl = file.getName();
	}

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
	    final String trackerPort = configuration.getProperty("tracker.port", "6969");
	    final String trackerIp = configuration.getProperty("tracker.ip", InetAddress.getLocalHost().getHostName());
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
