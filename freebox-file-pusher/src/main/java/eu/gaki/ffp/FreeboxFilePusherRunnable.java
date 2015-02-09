package eu.gaki.ffp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

/**
 * The Class FreeboxFilePusherRunnable.
 */
public class FreeboxFilePusherRunnable implements Runnable {

    /** The configuration. */
    private final Properties configuration;

    /** The tracker. */
    private final Tracker tracker;

    /** The torrent rss. */
    private final TorrentRss torrentRss = new TorrentRss();

    /**
     * Instantiates a new freebox file pusher runnable.
     *
     * @param configuration
     *            the configuration
     * @param tracker
     *            the tracker
     */
    public FreeboxFilePusherRunnable(final Properties configuration, final Tracker tracker) {
	this.configuration = configuration;
	this.tracker = tracker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
	try {
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
			// Add file to tracker
			final TrackedTorrent torrent = TrackedTorrent.load(torrentFile);
			torrent.setSeederRunnable(new InitialSeederRunnable(configuration, tracker, torrent, torrentFile, dataFile));
			tracker.announce(torrent);
		    }
		}
	    }


	} catch (final Exception e) {
	    // Throwed exception will stop the ScheduledExecutorService
	    e.printStackTrace();
	}
	// Publish RSS file with torrent files
	torrentRss.generateRss(configuration, tracker.getTrackedTorrents());
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
	OutputStream fos = null;
	try {
	    torrentFile = computeTorrentFileName(file);
	    fos = new FileOutputStream(torrentFile);

	    final URI announceURI = tracker.getAnnounceUrl().toURI();

	    final String creator = String.format("%s (ttorrent)",
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
	} catch (URISyntaxException | InterruptedException | IOException e) {
	    e.printStackTrace();
	} finally {
	    if (fos != null) {
		try {
		    fos.close();
		} catch (final IOException e) {
		    e.printStackTrace();
		}
	    }
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
	torrentFile = new File(
		configuration.getProperty("torrent.file.folder"),
		file.getName() + ".torrent");
	return torrentFile;
    }

}
