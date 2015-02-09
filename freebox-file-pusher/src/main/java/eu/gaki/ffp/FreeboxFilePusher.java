/**
 *
 */
package eu.gaki.ffp;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;

import com.turn.ttorrent.tracker.Tracker;

/**
 * The Class FreeboxFilePusher.
 *
 * @author Pilou
 */
public class FreeboxFilePusher implements Daemon {

    /** The Constant DEFAULT_REPEAT_INTERVAL. */
    private static final String DEFAULT_REPEAT_INTERVAL = "600";

    /** The watch executor. */
    private ScheduledExecutorService watchExecutor;

    /** The configuration. */
    private final Properties configuration = new Properties();

    /** The tracker. */
    private Tracker tracker;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final DaemonContext context) throws DaemonInitException,
    Exception {
	/*
	 * Construct objects and initialize variables here.
	 * You can access the command line arguments that would normally be passed to your main()
	 * method as follows:
	 */
	final String[] args = context.getArguments();

	String propertiesfileLocation = "/freeboxFilePusher.properties";
	if (args != null && args.length == 1) {
	    propertiesfileLocation = args[0];
	}
	final InputStream configurationInputStream = getClass().getResourceAsStream(propertiesfileLocation);
	if (configurationInputStream != null) {
	    configuration.load(configurationInputStream);
	}
	watchExecutor = Executors.newSingleThreadScheduledExecutor();
	final String trackerPort = configuration.getProperty("tracker.port", "6969");
	final String trackerIp = configuration.getProperty("tracker.ip", InetAddress.getLocalHost().getHostName());
	tracker = new Tracker(new InetSocketAddress(trackerIp,Integer.valueOf(trackerPort)), "FreeboxFilePusher");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws Exception {
	final String repeatInterval = configuration.getProperty("repeat.interval.seconds", DEFAULT_REPEAT_INTERVAL);
	watchExecutor.scheduleWithFixedDelay(new FreeboxFilePusherRunnable(configuration, tracker), 1, Long.valueOf(repeatInterval), TimeUnit.SECONDS);
	tracker.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
	watchExecutor.shutdown();
	tracker.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
	tracker = null;
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
		return null;
	    }
	};
	test.init(context);
	test.start();
    }

}
