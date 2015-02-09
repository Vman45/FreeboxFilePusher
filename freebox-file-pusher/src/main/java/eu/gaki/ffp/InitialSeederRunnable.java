package eu.gaki.ffp;

import java.io.File;
import java.net.InetAddress;
import java.util.Properties;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

/**
 * The Class InitialSeederRunnable.
 */
public class InitialSeederRunnable {

    /** The torrent. */
    private final TrackedTorrent torrent;

    /** The torrent file. */
    private final File torrentFile;

    /** The data file. */
    private final File dataFile;

    /** The tracker. */
    private final Tracker tracker;

    /** The seeder. */
    private Client seeder;

    /** The configuration. */
    private final Properties configuration;

    /** The total number of client. */
    private static volatile int totalNumberOfClient = 0;

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
    public InitialSeederRunnable(final Properties configuration, final Tracker tracker,
	    final TrackedTorrent torrent, final File torrentFile, final File dataFile) {
	this.configuration = configuration;
	this.tracker = tracker;
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
		System.err.println("START SEEDING " + sharedTorrent.getName() + " client number:"+totalNumberOfClient);
		seeder.share();
	    }
	} catch (final Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Checks if is seeding.
     *
     * @return true, if is seeding
     */
    public synchronized boolean isSeeding() {
	return seeder != null;
    }

    /**
     * Stop seeding.
     */
    public synchronized void stopSeeding() {
	try {
	    if (seeder != null) {
		System.err.println("STOP SEEDING " + torrent.getName() + " client number:"+totalNumberOfClient);
		seeder.stop();
		totalNumberOfClient -= 1;
	    }
	    tracker.remove(torrent);
	    dataFile.delete();
	    torrentFile.delete();
	    seeder = null;
	} catch (final Exception e) {
	    e.printStackTrace();
	}
    }

}
