package eu.gaki.ffp;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
	private List<FolderListener> listener = new ArrayList<>();

	/** The torrent rss. */
	private final RssFileGenerator rssFileGenerator = new RssFileGenerator();
	
	/**
	 * Instantiates a new freebox file pusher runnable.
	 *
	 * @param configuration
	 *            the configuration
	 * @param tracker
	 *            the tracker
	 */
	public FoldersWatcherRunnable(final Properties configuration) {
		this.configuration = configuration;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {

			List<RssFileItem> rssFileItems = new ArrayList<>();
			
			// Watch new files and folder in watched folder
			final String folderLocation = configuration.getProperty("folders.to.watch", null);
			if (folderLocation != null) {
				Path folder = FileSystems.getDefault().getPath(folderLocation);
				
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
				    for (Path file: stream) {
				    	for (FolderListener listener : listener) {
							List<RssFileItem> listenerRssFileItems = listener.folderFile(file, folder);
							if (listenerRssFileItems != null) {
								rssFileItems.addAll(listenerRssFileItems);
							}
						}
				    }
				} catch (IOException | DirectoryIteratorException e) {
				    LOGGER.error(e.getMessage(),e);;
				}
			}
			
			if (!rssFileItems.isEmpty()) {
				rssFileGenerator.generateRss(configuration, rssFileItems);
			}

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
	void addFolderListener(FolderListener folderListener) {
		listener.add(folderListener);
	}

}
