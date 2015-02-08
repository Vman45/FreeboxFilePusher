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

public class FreeboxFilePusherRunnable implements Runnable {

	private Properties configuration;
	private Tracker tracker;

	public FreeboxFilePusherRunnable(Properties configuration, Tracker tracker) {
		this.configuration = configuration;
		this.tracker = tracker;
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
						File torrentFile = computeTorrentFileName(dataFile);
						if (!torrentFile.exists()) {
							// Create torrent file
							torrentFile = createTorrentFile(dataFile);
						}
						// Add file to tracker
						TrackedTorrent torrent = TrackedTorrent.load(torrentFile);
						torrent.setSeederRunnable(new InitialSeederRunnable(configuration, tracker, torrent, torrentFile, dataFile));
						tracker.announce(torrent);
					}
				}
			}

			// Publish RSS file with torrent files

		} catch (Exception e) {
			// Throwed exception will stop the ScheduledExecutorService
		}
	}

	private File createTorrentFile(File file) {
		File torrentFile = null;
		OutputStream fos = null;
		try {
			torrentFile = computeTorrentFileName(file);
			fos = new FileOutputStream(torrentFile);

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
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return torrentFile;
	}

	private File computeTorrentFileName(File file) {
		File torrentFile;
		torrentFile = new File(
				configuration.getProperty("torrent.file.folder"),
				file.getName() + ".torrent");
		return torrentFile;
	}

}
