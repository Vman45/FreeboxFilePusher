package eu.gaki.ffp;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class FreeboxFilePusherRunnable.
 */
public class FoldersWatcherRunnable implements Runnable {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FoldersWatcherRunnable.class);

	/** The configuration. */
	private final Properties configuration;

	/** The listener. */
	private final List<FolderListener> listeners = new ArrayList<>();

	/** The torrent rss. */
	private final RssFileGenerator rssFileGenerator;

	/**
	 * Instantiates a new freebox file pusher runnable.
	 *
	 * @param configuration
	 *            the configuration
	 * @param rssFileGenerator
	 *            the rss file generator
	 */
	public FoldersWatcherRunnable(final Properties configuration, final RssFileGenerator rssFileGenerator) {
		this.configuration = configuration;
		this.rssFileGenerator = rssFileGenerator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {

			final Set<RssFileItem> rssFileItems = new HashSet<>();

			// Watch new files and folder in watched folder
			final String folderLocation = configuration.getProperty("folders.to.watch", null);
			if (folderLocation != null) {
				final Path folder = FileSystems.getDefault().getPath(folderLocation);
				listeners.forEach(listener -> listener.beginning(folder));

				try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
					for (final Path file : stream) {
						if (isFileFinishCopied(file)) {
							for (final FolderListener listener : listeners) {
								final Collection<RssFileItem> listenerRssFileItems = listener.folderFile(folder, file);
								if (listenerRssFileItems != null) {
									rssFileItems.addAll(listenerRssFileItems);
								}
							}
						}
					}
				} catch (IOException | DirectoryIteratorException e) {
					LOGGER.error(e.getMessage(), e);
				}

				listeners.forEach(listener -> listener.ending(folder));
			}

			rssFileGenerator.generateRss(configuration, rssFileItems);

		} catch (final Exception e) {
			LOGGER.error("Cannot watch folder:" + e.getMessage(), e);
		}
	}

	/**
	 * Checks if is file finish copied.
	 *
	 * @param file
	 *            the file
	 * @return true, if is file finish copied
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	private boolean isFileFinishCopied(final Path file) throws IOException, InterruptedException {
		final Map<Path, Long> sizes = new HashMap<Path, Long>();

		final AtomicBoolean finishCopied = new AtomicBoolean(true);
		Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				sizes.put(file, Files.size(file));
				return FileVisitResult.CONTINUE;
			}
		});
		Thread.sleep(500);
		Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				FileVisitResult result = FileVisitResult.CONTINUE;
				final Long initialSize = sizes.get(file);
				if (initialSize == null || !initialSize.equals(Files.size(file))) {
					finishCopied.set(false);
					result = FileVisitResult.TERMINATE;
				}
				return result;
			}
		});
		return finishCopied.get();
	}

	/**
	 * Adds the folder listener.
	 *
	 * @param folderListener
	 *            the folder listener
	 */
	void addFolderListener(final FolderListener folderListener) {
		listeners.add(folderListener);
	}

}
