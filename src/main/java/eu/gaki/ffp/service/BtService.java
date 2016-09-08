/**
 *
 */
package eu.gaki.ffp.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.Client.ClientState;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.StatusEnum;

// TODO: Auto-generated Javadoc
/**
 * The Class BtService.
 *
 * @author Pilou
 */
public class BtService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(BtService.class);

	/** The tracker. */
	private Tracker tracker;

	/** The tracker port. */
	private final Integer trackerPort;

	/** The tracker ip. */
	private final String trackerIp;

	/** The client ip. */
	private final String clientIp;

	/** The tracker announce url. */
	private final String trackerAnnounceUrl;

	/** The torrent file folder. */
	private final String torrentFileFolder;

	/** The torrent extension. */
	private final String torrentExtension;

	/** The executors. */
	private final ScheduledExecutorService executors = Executors.newScheduledThreadPool(10);

	/** The dao service. */
	private final DaoService daoService;

	/**
	 * Instantiates a new bt service.
	 *
	 * @param configService
	 *            the config service
	 * @param daoService
	 *            the dao service
	 */
	public BtService(final ConfigService configService, final DaoService daoService) {
		this.daoService = daoService;
		trackerPort = configService.getTorrentTrackerPort();
		trackerIp = configService.getTorrentTrackerIp();
		clientIp = configService.getTorrentClientIp();
		trackerAnnounceUrl = configService.getPublicUrlTrackerAnnounce();
		torrentFileFolder = configService.getTorrentFileFolder();
		torrentExtension = configService.getTorrentExtension();
	}

	/**
	 * Gets the torrent of an FfpItem by lazy loading.
	 *
	 * @param item
	 *            the item
	 * @return the torrent or null if no torrent associate
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected Torrent getTorrent(final FfpItem item) throws NoSuchAlgorithmException, IOException {
		final Torrent torrent;
		if (item.getTorrentPath() != null && Files.exists(item.getTorrentPath())) {
			torrent = Torrent.load(item.getTorrentPath().toFile(), true);
		} else {
			torrent = createTorrentFile(item);
		}
		return torrent;
	}

	/**
	 * Start sharing.
	 *
	 * @param item
	 *            the item
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void startSharing(final FfpItem item) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
		// Announce the torrent
		final Torrent torrent = getTorrent(item);
		startTracker();
		if (torrent != null && tracker != null && !isTracked(item)) {
			// Get the torrent

			// Add torrent to tracker
			final TrackedTorrent trackedTorrent = new TrackedTorrent(torrent);
			// final TrackedTorrent trackedTorrent =
			// TrackedTorrent.load(item.getTorrentPath().toFile());

			tracker.announce(trackedTorrent);
			LOGGER.info("Announce file: {}", item);

			// Start a seeder torrent
			final SharedTorrent sharedTorrent = new SharedTorrent(trackedTorrent,
					item.getFfpFiles().get(0).getPath().getParent().toFile(), true);
			final Client seeder = new Client(InetAddress.getByName(clientIp), sharedTorrent);
			executors.scheduleWithFixedDelay(() -> {
				if (ClientState.WAITING.equals(seeder.getState())) {
					seeder.share();
				}
				// If sending ended we shutdown the sender
				final long numberOfSeeder = trackedTorrent.getPeers().values().stream().filter(peer -> {
					// We do not count the seeder
					return !peer.looksLike(seeder.getPeerSpec());
				}).filter(peer -> peer.isCompleted()).count();
				if (numberOfSeeder > 0) {
					seeder.stop(true);
					tracker.remove(trackedTorrent);
					stopTracker();
					item.setStatus(StatusEnum.SENDED);
					daoService.save();
					LOGGER.info("Announce file remove: {}", item);
					// Throw exception for stop this schedule
					throw new RuntimeException("Stop watching for ending the seeder torrent");
				}
			}, 0, 1, TimeUnit.SECONDS);

		} else {
			LOGGER.error("No torrent file to share: {}", item);
		}

	}

	/**
	 * Checks if a torrent is already tracked.
	 *
	 * @param item
	 *            the item
	 * @return true, if is tracked
	 */
	protected boolean isTracked(final FfpItem item) {
		boolean result = false;

		for (final TrackedTorrent torrent : tracker.getTrackedTorrents()) {
			final List<String> filenames = torrent.getFilenames();
			for (final String string : filenames) {
				if (!item.contains(Paths.get(string)).isEmpty()) {
					result = true;
					break;
				}
			}
			if (result) {
				break;
			}
		}

		return result;
	}

	/**
	 * Start the tracker if not already started.
	 *
	 * @throws NumberFormatException
	 *             the number format exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected synchronized void startTracker() throws NumberFormatException, IOException {
		if (tracker == null) {
			tracker = new Tracker(new InetSocketAddress(trackerIp, trackerPort), "FreeboxFilePusher");
			tracker.start();
			LOGGER.info("Tracker starded {}:{}", trackerIp, trackerPort);
		}
	}

	/**
	 * Stop the tracker if started.
	 */
	protected synchronized void stopTracker() {
		if (tracker != null && tracker.getTrackedTorrents().isEmpty()) {
			tracker.stop();
			tracker = null;
			LOGGER.info("Tracker stoped");
		}
	}

	/**
	 * Creates the torrent file.
	 *
	 * @param item
	 *            the item
	 * @return the torrent
	 */
	protected Torrent createTorrentFile(final FfpItem item) {
		final Instant start = Instant.now();
		LOGGER.trace("Start create torrent of {}.", item);

		Torrent torrent = null;
		final Path torrentFile = computeTorrentFileName(item);
		try (OutputStream fos = new FileOutputStream(torrentFile.toFile())) {

			final List<List<URI>> announceURIs = new ArrayList<>();
			final List<URI> announceURI = new ArrayList<>();
			announceURI.add(new URI(trackerAnnounceUrl));
			announceURI.add(new URI("http", null, this.trackerIp, this.trackerPort, "/announce", null, null));

			announceURIs.add(announceURI);

			final String creator = String.format("%s (FreeboxFilePusher)", System.getProperty("user.name"));

			final File firstFile = item.getFfpFiles().get(0).getPath().toFile();
			if (item.getFfpFiles().size() > 1) {
				final List<File> files = item.getFfpFiles().stream().map(FfpFile::getPath)
						.filter(path -> !Files.isDirectory(path)).map(Path::toFile).collect(Collectors.toList());
				Collections.sort(files);
				torrent = Torrent.create(firstFile, files, Torrent.DEFAULT_PIECE_LENGTH, announceURIs, creator);
			} else if (item.getFfpFiles().size() == 1) {
				torrent = Torrent.create(firstFile, Torrent.DEFAULT_PIECE_LENGTH, announceURIs, creator);
			}
			torrent.save(fos);
			// item.setTorrent(torrent);
			item.setTorrentPath(torrentFile);
			LOGGER.info("Create torrent file: {}", torrentFile);
		} catch (URISyntaxException | InterruptedException | IOException | NoSuchAlgorithmException e) {
			LOGGER.error("Cannot create torrent file:" + e.getMessage(), e);
		}
		final Instant end = Instant.now();
		LOGGER.trace("Stop create torrent of {}. Took {}", item, Duration.between(start, end));

		return torrent;
	}

	/**
	 * Compute torrent file name.
	 *
	 * @param item
	 *            the item
	 * @return the file
	 */
	protected Path computeTorrentFileName(final FfpItem item) {
		Path torrentFile = null;

		if (item != null && item.getFfpFiles().size() > 0) {
			final String fileNameUrl = FilenameUtils
					.getBaseName(item.getFfpFiles().get(0).getPath().getFileName().toString());
			torrentFile = FileSystems.getDefault().getPath(torrentFileFolder, fileNameUrl + torrentExtension);
		}

		return torrentFile;
	}

}
