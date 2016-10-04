/*
 *
 */
package eu.gaki.ffp.runnable;

import java.time.Duration;
import java.time.LocalDateTime;
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
public class ArchivedWatcherRunnable implements Runnable {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ArchivedWatcherRunnable.class);

	/** The service provider. */
	private final ServiceProvider serviceProvider;

	/** The archived purge delay. */
	private final Long archivedPurgeDelay;

	/**
	 * Instantiates a new job for scan a watched folder.
	 *
	 * @param watchedFolder
	 *            the watched folder
	 * @param serviceProvider
	 *            the service provider
	 */
	public ArchivedWatcherRunnable(final ServiceProvider serviceProvider) {
		this.serviceProvider = serviceProvider;
		archivedPurgeDelay = serviceProvider.getConfigService().getArchivedPurgeDelay();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		try {
			final List<FfpItem> archiveds = serviceProvider.getDaoService().getByStatus(StatusEnum.ARCHIVED);

			final AtomicBoolean someItemChanged = new AtomicBoolean(false);

			archiveds.forEach(item -> {
				final LocalDateTime adler32Date = item.getAdler32Date();
				if (adler32Date != null) {
					final Duration between = Duration.between(adler32Date, LocalDateTime.now());
					if (between.getSeconds() >= archivedPurgeDelay) {
						serviceProvider.getDaoService().get().removeFfpItem(item);
						LOGGER.info("Remove ARCIHVED {} from system.", item);
						someItemChanged.set(true);
					}
				} else {
					// Non adler, it's strange so we remove
					serviceProvider.getDaoService().get().removeFfpItem(item);
					LOGGER.info("Remove ARCIHVED {} from system.", item);
					someItemChanged.set(true);
				}
			});

			// Save the domain if needed
			if (someItemChanged.get()) {
				serviceProvider.getDaoService().save();
			}
		} catch (final Exception e) {
			LOGGER.error("Cannot watch ARCHIVED status:" + e.getMessage(), e);
		}
	}

}
