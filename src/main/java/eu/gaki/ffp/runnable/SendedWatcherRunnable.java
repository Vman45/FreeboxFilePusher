/*
 *
 */
package eu.gaki.ffp.runnable;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.StatusEnum;
import eu.gaki.ffp.service.ServiceProvider;

/**
 * Job for scan a watched folder.
 */
public class SendedWatcherRunnable implements Runnable {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SendedWatcherRunnable.class);

	/** The service provider. */
	private final ServiceProvider serviceProvider;

	private final Boolean deleteAfterSending;

	/**
	 * Instantiates a new job for scan a watched folder.
	 *
	 * @param watchedFolder
	 *            the watched folder
	 * @param serviceProvider
	 *            the service provider
	 */
	public SendedWatcherRunnable(final ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
		deleteAfterSending = serviceProvider.getConfigService().isDeleteAfterSending();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			final List<FfpItem> toSends = serviceProvider.getDaoService().getByStatus(StatusEnum.SENDED);
			final AtomicBoolean someItemChanged = new AtomicBoolean(false);
			toSends.forEach(item -> {
				try {
					// Delete torrent file
					if (item.getTorrentPath() != null && Files.exists(item.getTorrentPath())) {
						Files.delete(item.getTorrentPath());
						item.setTorrentPathUri(null);
						LOGGER.info("Delete torrent for {}.", item);
					}

					// Delete Item as well if asked
					if (deleteAfterSending) {
						serviceProvider.getItemService().delete(item);
						item.setStatus(StatusEnum.ARCHIVED);
						LOGGER.info("Set status ARCHIVED for {}.", item);
						someItemChanged.set(true);
					}
				} catch (final Exception e) {
					LOGGER.error("Cannot delete " + item, e);
				}
			});

			// Save the domain if needed
			if (someItemChanged.get()) {
				serviceProvider.getDaoService().save();
			}
		} catch (final Exception e) {
			LOGGER.error("Cannot watch SENDED status:" + e.getMessage(), e);
		}
	}

}
