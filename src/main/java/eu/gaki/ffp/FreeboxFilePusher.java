/*
 *
 */
package eu.gaki.ffp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.RssFileItem;
import eu.gaki.ffp.runnable.FolderWatcherRunnable;
import eu.gaki.ffp.runnable.ToSendWatcherRunnable;
import eu.gaki.ffp.service.ServiceProvider;

/**
 * The Class FreeboxFilePusher.
 *
 * @author Pilou
 */
public class FreeboxFilePusher implements Daemon {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FreeboxFilePusher.class);

	/** The watch executor. */
	private ScheduledExecutorService watchExecutor;

	private ServiceProvider serviceProvider;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final DaemonContext context) throws DaemonInitException, Exception {
		LOGGER.trace("Initialize...");

		System.setProperty("file.encoding", "UTF-8");
		Locale.setDefault(Locale.FRANCE);

		// Construct objects and initialize variables here. You can access the
		// command line arguments that would normally be passed to your main()
		// method as follows

		// Create executor
		watchExecutor = Executors.newScheduledThreadPool(5);

		final String[] args = context.getArguments();
		if (args != null && args.length > 0) {
			serviceProvider = new ServiceProvider(args[0]);
		} else {
			serviceProvider = new ServiceProvider();
		}

		// Initialize the rss feed to empty
		// FIXME Why ?
		serviceProvider.getRssFileGenerator().generateRss(new ArrayList<RssFileItem>());

		// Add configured listener
		// if (configService.isEnableBittorent()) {
		// foldersWatcherRunnable
		// .addFolderListener(new
		// eu.gaki.ffp.bittorrent.BittorrentFolderListener(
		// null));
		// }

		// if (configService.isEnableHttp()) {
		// foldersWatcherRunnable
		// .addFolderListener(new eu.gaki.ffp.http.HttpFolderListener(
		// null));
		// }

		// // Get RSS items
		// final Set<RssFileItem> rssFileItems = new HashSet<>();
		// listeners.forEach(listener -> {
		// rssFileItems.addAll(listener.getRssItemList());
		// });

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() throws Exception {
		LOGGER.trace("Start watching...");
		// Read the watched folder list
		final List<Path> foldersToWatch = serviceProvider.getConfigService().getFoldersToWatch();
		final Long repeatInterval = serviceProvider.getConfigService().getRepeatInterval();
		for (final Path path : foldersToWatch) {
			final FolderWatcherRunnable foldersWatcherRunnable = new FolderWatcherRunnable(path, serviceProvider);
			watchExecutor.scheduleWithFixedDelay(foldersWatcherRunnable, 0, repeatInterval, TimeUnit.SECONDS);
		}
		watchExecutor.scheduleWithFixedDelay(new ToSendWatcherRunnable(serviceProvider), 30, repeatInterval,
				TimeUnit.SECONDS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception {
		LOGGER.trace("Stop watching...");
		watchExecutor.shutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		LOGGER.trace("Destroy watching...");
		watchExecutor = null;
		serviceProvider = null;
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws DaemonInitException
	 *             the daemon init exception
	 * @throws Exception
	 *             the exception
	 */
	public static void main(final String[] args) throws DaemonInitException, Exception {
		final FreeboxFilePusher test = new FreeboxFilePusher();
		final DaemonContext context = new DaemonContext() {
			@Override
			public DaemonController getController() {
				return null;
			}

			@Override
			public String[] getArguments() {
				return args;
			}
		};
		test.init(context);
		test.start();
	}

}
