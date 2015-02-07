package eu.gaki.ffp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

public class InitialSeederRunnabe implements Runnable {
	
	private TrackedTorrent torrent;
	private File torrentFile;
	private File dataFile;
	private Tracker tracker;

	public InitialSeederRunnabe(Tracker tracker, TrackedTorrent torrent, File torrentFile, File dataFile) {
		this.tracker = tracker;
		this.torrent = torrent;
		this.torrentFile = torrentFile;
		this.dataFile = dataFile;
	}

	@Override
	public void run() {
		try {
			SharedTorrent sharedTorrent = new SharedTorrent(torrent, dataFile.getParentFile(), true);
			Client seeder = new Client(InetAddress.getLocalHost(), sharedTorrent);
			seeder.share();
			while (true) {
				long uploaded = sharedTorrent.getUploaded();
				long size = sharedTorrent.getSize();
				if (uploaded >= size) {
					seeder.stop();
					torrentFile.delete();
					dataFile.delete();
					tracker.remove(torrent);
					break;
				}
				Thread.sleep(10000);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
