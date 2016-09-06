/*
 *
 */
package eu.gaki.ffp.runnable;

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
public class ToSendWatcherRunnable implements Runnable {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ToSendWatcherRunnable.class);

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
	public ToSendWatcherRunnable(final ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			final List<FfpItem> toSends = serviceProvider.getDaoService().getByStatus(StatusEnum.TO_SEND);

			toSends.forEach(item -> {
				try {
					serviceProvider.getBtService().startSharing(item);
					item.setStatus(StatusEnum.SENDING);
				} catch (final Exception e) {
					LOGGER.error("Cannot startSharing " + item, e);
				}
			});

			// Save the domain
			serviceProvider.getDaoService().save();
		} catch (final Exception e) {
			LOGGER.error("Cannot watch TO_SEND status:" + e.getMessage(), e);
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
