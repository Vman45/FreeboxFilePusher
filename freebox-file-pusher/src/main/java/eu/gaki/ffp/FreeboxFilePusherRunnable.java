package eu.gaki.ffp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

public class FreeboxFilePusherRunnable implements Runnable {

	private Properties configuration;
	private Tracker tracker;
	private ExecutorService initialSeederPool;

	public FreeboxFilePusherRunnable(Properties configuration, Tracker tracker, ExecutorService initialSeederPool) {
		this.configuration = configuration;
		this.tracker = tracker;
		this.initialSeederPool = initialSeederPool;
	}

	@Override
	public void run() {
		try {
			// Watch new files and folder in watched folder
			String folderLocation = configuration.getProperty(
					"folders.to.watch", null);
			if (folderLocation != null) {
				File folder = new File(folderLocation);
				if (folder.isDirectory()) {
					File[] list = folder.listFiles();
					for (File dataFile : list) {
						if (!isFileInTracker(dataFile)) {
							// Create torrent file
							File torrentFile = createTorrentFile(dataFile);
							if (torrentFile != null) {
								// Add file to tracker
								TrackedTorrent torrent = TrackedTorrent.load(torrentFile);
								tracker.announce(torrent);
								// Create initial seeder
								Thread t = new Thread(new InitialSeederRunnabe(tracker, torrent, torrentFile, dataFile), torrent.getName());
								t.start();
//								initialSeederPool.execute();
							}
						}
					}
				}
			}

			// Delete torrent files which files no more exist

			// Create torrent files for each files and folders in watched folder

			// Add torrent files to tracker

			// Publish RSS file with torrent files

		} catch (Exception e) {
			// Throwed exception will stop the ScheduledExecutorService
		}
	}

	private File createTorrentFile(File file) {
		File torrentFile = null;
		try {
			torrentFile = new File(
					configuration.getProperty("torrent.file.folder"),
					file.getName() + ".torrent");
			OutputStream fos = new FileOutputStream(torrentFile);

//			URI announceURI = new URI(
//					configuration.getProperty("torrent.announce.url"));
			URI announceURI = tracker.getAnnounceUrl().toURI();

			String creator = String.format("%s (ttorrent)",
					System.getProperty("user.name"));

			Torrent torrent = null;
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				Arrays.sort(files);
				torrent = Torrent.create(file, Arrays.asList(files),
						announceURI, creator);
			} else {
				torrent = Torrent.create(file, announceURI, creator);
			}

			torrent.save(fos);
		} catch (URISyntaxException | InterruptedException | IOException e) {
			e.printStackTrace();
		}

		return torrentFile;
	}

	/**
	 * Check if a file is already in tracker
	 * 
	 * @param file
	 *            the File to search
	 * @return true if File is found in tracker
	 */
	private boolean isFileInTracker(File file) {
		boolean result = false;
		Collection<TrackedTorrent> trackedTorrents = tracker
				.getTrackedTorrents();
		for (TrackedTorrent trackedTorrent : trackedTorrents) {
			if (trackedTorrent.getName().equals(file.getName())) {
				result = true;
				break;
			}
		}
		return result;
	}

}
