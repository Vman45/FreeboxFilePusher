/**
 *
 */
package eu.gaki.ffp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.bittorrent.BittorrentFolderListener;
import eu.gaki.ffp.http.HttpFolderListener;

/**
 * The Class FreeboxFilePusher.
 *
 * @author Pilou
 */
public class FreeboxFilePusher implements Daemon {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FreeboxFilePusher.class);

	/** The Constant DEFAULT_REPEAT_INTERVAL. */
	private static final String DEFAULT_REPEAT_INTERVAL = "600";

	/** The watch executor. */
	private ScheduledExecutorService watchExecutor;

	/** The configuration. */
	private final Properties configuration = new Properties();

	/** The folders watcher runnable. */
	private FoldersWatcherRunnable foldersWatcherRunnable;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final DaemonContext context) throws DaemonInitException, Exception {

		// Construct objects and initialize variables here. You can access the
		// command line arguments that would normally be passed to your main()
		// method as follows
		final String[] args = context.getArguments();

		// Load configuration file
		loadConfigurationFile(args);

		// Create executor
		watchExecutor = Executors.newSingleThreadScheduledExecutor();

		// Create foldersWatcherRunnable
		foldersWatcherRunnable = new FoldersWatcherRunnable(configuration);

		// Add configured listener
		foldersWatcherRunnable.addFolderListener(new BittorrentFolderListener(configuration));
		foldersWatcherRunnable.addFolderListener(new HttpFolderListener(configuration));

	}

	/**
	 * Load configuration file.
	 *
	 * @param args
	 *            the args
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void loadConfigurationFile(final String[] args) throws IOException {
		String propertiesfileLocation = "freeboxFilePusher.properties";
		if (args != null && args.length == 1) {
			propertiesfileLocation = args[0];
		}
		try (InputStream configurationInputStream = getConfigurationInputStream(propertiesfileLocation)) {
			// Try to load from disk
			if (configurationInputStream != null) {
				configuration.load(configurationInputStream);
			}
		} catch (final IOException e) {
			LOGGER.error("Cannot load configuration file: " + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Gets the configuration input stream.
	 *
	 * @param propertiesfileLocation
	 *            the propertiesfile location
	 * @return the configuration input stream
	 * @throws FileNotFoundException
	 *             the file not found exception
	 */
	private InputStream getConfigurationInputStream(final String propertiesfileLocation) throws FileNotFoundException {
		InputStream configurationInputStream = null;
		final File diskFile = new File(propertiesfileLocation);
		if (diskFile.isFile()) {
			configurationInputStream = new FileInputStream(diskFile);
		} else if (getClass().getResourceAsStream(propertiesfileLocation) != null) {
			// Try to load from classpath
			configurationInputStream = getClass().getResourceAsStream(propertiesfileLocation);
		}
		return configurationInputStream;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() throws Exception {
		final String repeatInterval = configuration.getProperty("folder.scan.interval.seconds", DEFAULT_REPEAT_INTERVAL);
		watchExecutor.scheduleWithFixedDelay(foldersWatcherRunnable, 10, Long.valueOf(repeatInterval), TimeUnit.SECONDS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws Exception {
		watchExecutor.shutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		watchExecutor = null;
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
