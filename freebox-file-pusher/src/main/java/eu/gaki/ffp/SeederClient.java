package eu.gaki.ffp;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.tracker.TrackedTorrent;

/**
 * The Class InitialSeederRunnable.
 */
public class SeederClient {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SeederClient.class);

    /** The torrent. */
    private final TrackedTorrent torrent;

    /** The torrent file. */
    private final File torrentFile;

    /** The data file. */
    private final File dataFile;

    /** The seeder. */
    private Client seeder;

    /** The configuration. */
    private final Properties configuration;

    /** The total number of client. */
    private static volatile int totalNumberOfClient = 0;

    /** The no leecher date. */
    private Date noLeecherDate = null;

    /**
     * Instantiates a new initial seeder runnable.
     *
     * @param configuration
     *            the configuration
     * @param tracker
     *            the tracker
     * @param torrent
     *            the torrent
     * @param torrentFile
     *            the torrent file
     * @param dataFile
     *            the data file
     */
    public SeederClient(final Properties configuration, final TrackedTorrent torrent, final File torrentFile,
	    final File dataFile) {
	this.configuration = configuration;
	this.torrent = torrent;
	this.torrentFile = torrentFile;
	this.dataFile = dataFile;
    }

    /**
     * Start seeding.
     */
    public synchronized void startSeeding() {
	try {
	    if (totalNumberOfClient < 6 && seeder == null && dataFile.exists()) {
		totalNumberOfClient += 1;
		final SharedTorrent sharedTorrent = new SharedTorrent(torrent,
			dataFile.getParentFile(), true);
		final String trackerIp = configuration.getProperty("tracker.ip",
			InetAddress.getLocalHost().getHostName());
		seeder = new Client(InetAddress.getByName(trackerIp),
			sharedTorrent);
		seeder.share();
		LOGGER.info("Started seeding {}. Number of client {}", torrent.getName(), totalNumberOfClient);
	    }
	} catch (final Exception e) {
	    LOGGER.error("Cannot start seeding:" + e.getMessage(), e);
	}
    }

    /**
     * Stop seeding.
     */
    public synchronized void stopSeeding() {
	try {
	    if (seeder != null) {
		seeder.stop();
		totalNumberOfClient -= 1;
		LOGGER.info("Stoped seeding {}. Number of client {}", torrent.getName(), totalNumberOfClient);
		seeder = null;
	    }
	} catch (final Exception e) {
	    LOGGER.error("Cannot stop seeding:" + e.getMessage(), e);
	}
    }

    /**
     * Stop and delete.
     */
    public synchronized void stopAndDelete() {
	try {
	    stopSeeding();
	    final String deleteString = configuration.getProperty("delete.after.sending", "false");
	    if (Boolean.valueOf(deleteString)) {
		dataFile.delete();
		torrentFile.delete();
		LOGGER.info("Delete torrent, file and remove from tracker {}. Number of client {}", torrent.getName(),
			totalNumberOfClient);
	    }
	} catch (final Exception e) {
	    LOGGER.error("Cannot stop seeding:" + e.getMessage(), e);
	}
    }

    /**
     * Enable disable seeding.
     *
     * @param torrent
     *            the torrent
     */
    public void enableDisableSeeding(final TrackedTorrent torrent) {
	// Enable disable seeder client on demand
	// A peer want to download the torrent
	if (torrent.leechers() > 0 && seeder == null) {
	    LOGGER.info(torrentFile.getPath() + ": {} leechers, {} seeders", torrent.leechers(), torrent.seeders());
	    // No seeder for this torrent, we create a client seeder
	    startSeeding();
	} else if (torrent.leechers() == 0 && seeder != null) {
	    // No more leecher
	    if (noLeecherDate == null) {
		LOGGER.info(torrentFile.getPath() + ": {} leechers, {} seeders", torrent.leechers(), torrent.seeders());
		noLeecherDate = new Date();
		LOGGER.info("No more leecher for file: {}", torrentFile.getPath());
	    }

	    if (torrent.seeders() >= 2) {
		LOGGER.info(torrentFile.getPath() + ": {} leechers, {} seeders", torrent.leechers(), torrent.seeders());
		// File is sent completely
		stopAndDelete();
		noLeecherDate = null;
		torrent.setSeederClient(null);
	    } else {
		// One one seeder (us) probably the bittorent client put the torrent in stalled state (he don't yet see
		// the seeder) so we wait
		final String keepActiveString = configuration.getProperty("keep.seeder.active.millisecond", "600000");
		if (System.currentTimeMillis() > noLeecherDate.getTime() + Long.valueOf(keepActiveString)) {
		    LOGGER.info(torrentFile.getPath() + ": {} leechers, {} seeders", torrent.leechers(),
			    torrent.seeders());
		    stopSeeding();
		    noLeecherDate = null;
		}
	    }
	}

    }

    /**
     * Gets the torrent.
     *
     * @return the torrent
     */
    public TrackedTorrent getTorrent() {
	return torrent;
    }

    /**
     * Gets the torrent file.
     *
     * @return the torrent file
     */
    public File getTorrentFile() {
	return torrentFile;
    }

    /**
     * Gets the data file.
     *
     * @return the data file
     */
    public File getDataFile() {
	return dataFile;
    }

}
