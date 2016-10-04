/*
 *
 */
package eu.gaki.ffp.runnable;

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

			final AtomicBoolean someItemChanged = new AtomicBoolean(false);

			toSends.forEach(item -> {
				try {
					serviceProvider.getBtService().startSharing(item);
					item.setStatus(StatusEnum.SENDING);
					LOGGER.info("Set status SENDING for {}.", item);
					someItemChanged.set(true);
				} catch (final Exception e) {
					LOGGER.error("Cannot startSharing " + item, e);
				}
			});

			// Update the RSS feed
			serviceProvider.getRssFileGenerator()
					.generateRss(serviceProvider.getDaoService().getByStatus(StatusEnum.SENDING));

			// Save the domain if needed
			if (someItemChanged.get()) {
				serviceProvider.getDaoService().save();
			}
		} catch (final Exception e) {
			LOGGER.error("Cannot watch TO_SEND status:" + e.getMessage(), e);
		}
	}

}
