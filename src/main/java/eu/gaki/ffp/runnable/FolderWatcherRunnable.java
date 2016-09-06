/*
 *
 */
package eu.gaki.ffp.runnable;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.FolderListener;
import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.StatusEnum;
import eu.gaki.ffp.service.ServiceProvider;

/**
 * Job for scan a watched folder.
 */
public class FolderWatcherRunnable implements Runnable {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FolderWatcherRunnable.class);

	/** The folders to watch. */
	private final Path watchedFolder;

	/** The service provider. */
	private final ServiceProvider serviceProvider;

	/**
	 * Instantiates a new job for scan a watched folder.
	 *
	 * @param watchedFolder
	 *            the watched folder
	 * @param serviceProvider
	 *            the service provider
	 */
	public FolderWatcherRunnable(final Path watchedFolder, final ServiceProvider serviceProvider) {
		this.watchedFolder = watchedFolder;
		this.serviceProvider = serviceProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {

			// Watch new files and folder in watched folder
			if (Files.exists(watchedFolder)) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(watchedFolder)) {
					final Long fileChangeCooldown = serviceProvider.getConfigService().getFileChangeCooldown();
					for (final Path path : stream) {
						// Search for the path
						final List<FfpItem> contains = serviceProvider.getDaoService().contains(path.toUri());
						if (contains.isEmpty()) {
							// Create a new item and compute the checksum
							final FfpItem item = serviceProvider.getItemService().create(path);
							serviceProvider.getDaoService().get().addFfpItem(item);
							serviceProvider.getChecksumService().computeChecksum(item);
						} else {
							// Existing Item
							contains.forEach(item -> {
								if (StatusEnum.WATCH.equals(item.getStatus())) {
									// Update the item in case of files
									// added/removed
									final boolean filesChanged = serviceProvider.getItemService().update(item);
									// Update the Checksum
									final boolean checksumChanged = serviceProvider.getChecksumService()
											.computeChecksum(item);
									if (!filesChanged && !checksumChanged) {
										final LocalDateTime adler32Date = item.getAdler32Date();
										if (adler32Date != null) {
											final Duration between = Duration.between(adler32Date, LocalDateTime.now());
											if (between.getSeconds() >= fileChangeCooldown) {
												// No change during the cooldown
												// period:
												// we mark as to send
												item.setStatus(StatusEnum.TO_SEND);
												LOGGER.info(
														"Change status of {} to TO_SEND. Checksum don't have change for {} sec.",
														item, between.getSeconds());
											}
										}
									}
								}
							});
						}
					}
					// Save the domain
					serviceProvider.getDaoService().save();
				} catch (final Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			} else {
				LOGGER.warn("The folder {} doesn't exist.", watchedFolder);
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
	void addFolderListener(final FolderListener folderListener) {
		// listeners.add(folderListener);
	}

}
