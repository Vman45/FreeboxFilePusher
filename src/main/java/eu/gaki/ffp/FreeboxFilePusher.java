/*
 *
 */
package eu.gaki.ffp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import eu.gaki.ffp.service.ConfigService;
import eu.gaki.ffp.service.RssService;

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

    /** The folders watcher runnable. */
    private FoldersWatcherRunnable foldersWatcherRunnable;

    /** The config service. */
    private ConfigService configService;

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
	final String[] args = context.getArguments();

	// Load configuration file
	final Path configPath = Paths.get("freeboxFilePusher.properties");
	configService = new ConfigService(configPath);

	// Create executor
	watchExecutor = Executors.newSingleThreadScheduledExecutor();

	// Create Rss generator
	final RssService rssFileGenerator = new RssService(configService);
	// Initialize the rss feed to empty
	rssFileGenerator.generateRss(new ArrayList<RssFileItem>());

	// Create foldersWatcherRunnable
	foldersWatcherRunnable = new FoldersWatcherRunnable(null, rssFileGenerator);

	// Add configured listener
	if (configService.isEnableBittorent()) {
	    foldersWatcherRunnable.addFolderListener(new eu.gaki.ffp.bittorrent.BittorrentFolderListener(null));
	}

	if (configService.isEnableHttp()) {
	    foldersWatcherRunnable.addFolderListener(new eu.gaki.ffp.http.HttpFolderListener(null));
	}

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {
	LOGGER.trace("Start watching...");
	watchExecutor.scheduleWithFixedDelay(foldersWatcherRunnable, 10, configService.getRepeatInterval(),
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
