/*
 * 
 */
package eu.gaki.ffp.runnable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.FolderListener;

/**
 * The Class FolderWatcherRunnable.
 */
public class FolderWatcherRunnable implements Runnable {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FolderWatcherRunnable.class);

	/** The listener. */
	private final List<FolderListener> listeners = new ArrayList<>();

	/** The folders to watch. */
	private final Path watchedFolder;

	/**
	 * Instantiates a new freebox file pusher runnable.
	 *
	 * @param configuration
	 *            the configuration
	 * @param rssFileGenerator
	 *            the rss file generator
	 */
	public FolderWatcherRunnable(final Path watchedFolder) {
		this.watchedFolder = watchedFolder;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {

			// Watch new files and folder in watched folder
			if (Files.exists(watchedFolder)) {
				try (DirectoryStream<Path> stream = Files
						.newDirectoryStream(watchedFolder)) {

					for (final Path path : stream) {
						// Is this path already pushed
						boolean alreadyPushed = false;
						for (final FolderListener listener : listeners) {
							if (listener.isAlreadyPushed(path)) {
								alreadyPushed = true;
								break;
							}
						}

						if (!alreadyPushed) {
							for (final FolderListener listener : listeners) {
								listener.scanPath(watchedFolder, path);
							}
						}
					}
				} catch (final Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			} else {
				LOGGER.warn("The folder " + watchedFolder + " doesn't exist.");
			}

			// Call the after scans method
			listeners.forEach(listener -> listener.afterScans());

		} catch (final Exception e) {
			LOGGER.error("Cannot watch folder:" + e.getMessage(), e);
		}
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
