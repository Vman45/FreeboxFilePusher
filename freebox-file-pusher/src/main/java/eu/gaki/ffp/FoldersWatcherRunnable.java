package eu.gaki.ffp;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
	 * @param configuration            the configuration
	 * @param rssFileGenerator the rss file generator
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
				    for (final Path file: stream) {
				    	for (final FolderListener listener : listeners) {
							final Collection<RssFileItem> listenerRssFileItems = listener.folderFile(folder, file);
							if (listenerRssFileItems != null) {
								rssFileItems.addAll(listenerRssFileItems);
							}
						}
				    }
				} catch (IOException | DirectoryIteratorException e) {
				    LOGGER.error(e.getMessage(),e);;
				}
				
				listeners.forEach(listener -> listener.ending(folder));
			}
			
			rssFileGenerator.generateRss(configuration, rssFileItems);
			
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
