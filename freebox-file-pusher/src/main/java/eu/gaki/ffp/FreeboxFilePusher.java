package eu.gaki.ffp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
	private Properties configuration;

	/** The folders watcher runnable. */
	private FoldersWatcherRunnable foldersWatcherRunnable;

	/** The rss file generator. */
	private RssFileGenerator rssFileGenerator;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final DaemonContext context) throws DaemonInitException, Exception {
		LOGGER.trace("Initialize...");
		// Construct objects and initialize variables here. You can access the
		// command line arguments that would normally be passed to your main()
		// method as follows
		final String[] args = context.getArguments();

		// Load configuration file
		loadConfigurationFile(args);

		// Create executor
		watchExecutor = Executors.newSingleThreadScheduledExecutor();

		// Create Rss generator
		rssFileGenerator = new RssFileGenerator();
		// Initialize the rss feed to empty
		rssFileGenerator.generateRss(configuration, new ArrayList<RssFileItem>());

		// Create foldersWatcherRunnable
		foldersWatcherRunnable = new FoldersWatcherRunnable(configuration, rssFileGenerator);

		// Add configured listener
		final String enableBittorent = configuration.getProperty("ffp.enable.bittorrent", "false");
		final String enableHttp = configuration.getProperty("ffp.enable.http", "true");

		if (Boolean.valueOf(enableBittorent)) {
			foldersWatcherRunnable.addFolderListener(new eu.gaki.ffp.bittorrent.BittorrentFolderListener(configuration));
		}

		if (Boolean.valueOf(enableHttp)) {
			foldersWatcherRunnable.addFolderListener(new eu.gaki.ffp.http.HttpFolderListener(configuration));
		}

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
		configuration = new Properties();
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
			LOGGER.error("Cannot load configuration file: {}", e.getMessage(), e);
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
		LOGGER.trace("Start watching...");
		final String repeatInterval = configuration.getProperty("folder.scan.interval.seconds", DEFAULT_REPEAT_INTERVAL);
		watchExecutor.scheduleWithFixedDelay(foldersWatcherRunnable, 10, Long.valueOf(repeatInterval), TimeUnit.SECONDS);
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
		configuration = null;
		foldersWatcherRunnable = null;
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
