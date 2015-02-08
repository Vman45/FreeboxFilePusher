/**
 * 
 */
package eu.gaki.ffp;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;

import com.turn.ttorrent.tracker.Tracker;

/**
 * @author Pilou
 */
public class FreeboxFilePusher implements Daemon {

	private static final String DEFAULT_REPEAT_INTERVAL = "600";
	private ScheduledExecutorService watchExecutor;
	private Properties configuration = new Properties();
	private Tracker tracker;
//	private static ExecutorService seedingClientPool = Executors.newFixedThreadPool(9);

	@Override
	public void init(DaemonContext context) throws DaemonInitException,
			Exception {
		/*
         * Construct objects and initialize variables here.
         * You can access the command line arguments that would normally be passed to your main() 
         * method as follows:
         */
        String[] args = context.getArguments();
        
        String propertiesfileLocation = "/freeboxFilePusher.properties";
        if (args != null && args.length == 1) {
        	propertiesfileLocation = args[0];
        }
		InputStream configurationInputStream = getClass().getResourceAsStream(propertiesfileLocation);
		if (configurationInputStream != null) {
			configuration.load(configurationInputStream);
		}
		watchExecutor = Executors.newSingleThreadScheduledExecutor();
		String trackerPort = configuration.getProperty("tracker.port", "6969");
        String trackerIp = configuration.getProperty("tracker.ip", InetAddress.getLocalHost().getHostName());
		tracker = new Tracker(new InetSocketAddress(trackerIp,Integer.valueOf(trackerPort)), "FreeboxFilePusher");
	}

	@Override
	public void start() throws Exception {
		String repeatInterval = configuration.getProperty("repeat.interval.seconds", DEFAULT_REPEAT_INTERVAL);
		watchExecutor.scheduleWithFixedDelay(new FreeboxFilePusherRunnable(configuration, tracker), 1, Long.valueOf(repeatInterval), TimeUnit.SECONDS);
		tracker.start();
	}

	@Override
	public void stop() throws Exception {
		watchExecutor.shutdown();
		tracker.stop();
	}

	@Override
	public void destroy() {
		tracker = null;
		watchExecutor = null;
	}
	
	public static void main(String[] args) throws DaemonInitException, Exception {
		FreeboxFilePusher test = new FreeboxFilePusher();
		DaemonContext context = new DaemonContext() {
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
	
//	public static void addSeederClient(InitialSeederRunnable seederRunnable) {
//		seedingClientPool.execute(seederRunnable);
//	}

}
