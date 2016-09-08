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

import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.StatusEnum;
import eu.gaki.ffp.runnable.FolderWatcherRunnable;
import eu.gaki.ffp.runnable.SendedWatcherRunnable;
import eu.gaki.ffp.runnable.ToSendWatcherRunnable;
import eu.gaki.ffp.service.ServiceProvider;

// TODO: Auto-generated Javadoc
/**
 * The Class FreeboxFilePusher.
 *
 * @author Pilou
 */
public class FreeboxFilePusher implements Daemon {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FreeboxFilePusher.class);

	/** The watch folder executor. */
	private ScheduledExecutorService watchFolderExecutor;

	/** The watch status executor. */
	private ScheduledExecutorService watchStatusExecutor;

	/** The service provider. */
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
		watchFolderExecutor = Executors.newScheduledThreadPool(10);
		watchStatusExecutor = Executors.newScheduledThreadPool(2);

		final String[] args = context.getArguments();
		if (args != null && args.length > 0) {
			serviceProvider = new ServiceProvider(args[0]);
		} else {
			serviceProvider = new ServiceProvider();
		}

		// Relaunch all SENDING item
		final List<FfpItem> sendingItems = serviceProvider.getDaoService().getByStatus(StatusEnum.SENDING);
		sendingItems.forEach(item -> item.setStatus(StatusEnum.TO_SEND));
		serviceProvider.getDaoService().save();

		// Initialize the rss feed to empty for waiting an update
		serviceProvider.getRssFileGenerator().generateRss(new ArrayList<FfpItem>());
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
			watchFolderExecutor.scheduleWithFixedDelay(foldersWatcherRunnable, 0, repeatInterval, TimeUnit.SECONDS);
		}

		// Watcher of item status
		watchStatusExecutor.scheduleWithFixedDelay(new ToSendWatcherRunnable(serviceProvider), 15, repeatInterval,
				TimeUnit.SECONDS);
		watchStatusExecutor.scheduleWithFixedDelay(new SendedWatcherRunnable(serviceProvider), 30, repeatInterval,
				TimeUnit.SECONDS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception {
		LOGGER.trace("Stop watching...");
		watchFolderExecutor.shutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		LOGGER.trace("Destroy watching...");
		watchFolderExecutor = null;
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
