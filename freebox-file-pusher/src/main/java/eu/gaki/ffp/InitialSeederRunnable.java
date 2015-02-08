package eu.gaki.ffp;

import java.io.File;
import java.net.InetAddress;
import java.util.Properties;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

public class InitialSeederRunnable /*implements Runnable*/ {

	private TrackedTorrent torrent;
	private File torrentFile;
	private File dataFile;
	private Tracker tracker;
	private Client seeder;
	private Properties configuration;
	private static volatile int totalNumberOfClient = 0; 

	public InitialSeederRunnable(Properties configuration, Tracker tracker,
			TrackedTorrent torrent, File torrentFile, File dataFile) {
		this.configuration = configuration;
		this.tracker = tracker;
		this.torrent = torrent;
		this.torrentFile = torrentFile;
		this.dataFile = dataFile;
	}

//	@Override
//	public void run() {
//		startSeeding();
//	}

	public synchronized void startSeeding() {
		try {
			if (totalNumberOfClient < 6 && seeder == null && dataFile.exists()) {
				totalNumberOfClient += 1; 
				SharedTorrent sharedTorrent = new SharedTorrent(torrent,
						dataFile.getParentFile(), true);
				String trackerIp = configuration.getProperty("tracker.ip",
						InetAddress.getLocalHost().getHostName());
				seeder = new Client(InetAddress.getByName(trackerIp),
						sharedTorrent);
				System.err.println("START SEEDING " + sharedTorrent.getName() + " client number:"+totalNumberOfClient);
				seeder.share();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized boolean isSeeding() {
		return seeder != null;
	}

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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
