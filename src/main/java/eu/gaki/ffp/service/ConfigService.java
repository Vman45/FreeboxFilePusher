/*
 *
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * Use for get configuration parameters.
 */
public class ConfigService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

	/** The configuration. */
	private Properties configuration;

	/**
	 * Instantiates a new config service.
	 *
	 * @param configFile
	 *            the config file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public ConfigService(final Path configFile) throws IOException {
		loadConfigurationFile(configFile);
	}

	/**
	 * Load configuration file.
	 *
	 * @param fileLocation
	 *            the file location
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void loadConfigurationFile(final Path fileLocation) throws IOException {
		configuration = new Properties();
		Path propertiesfileLocation;
		if (fileLocation != null) {
			propertiesfileLocation = fileLocation;
		} else {
			propertiesfileLocation = Paths.get("freeboxFilePusher.properties");
		}

		try (InputStream configurationInputStream = getConfigurationInputStream(propertiesfileLocation)) {
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
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private InputStream getConfigurationInputStream(final Path propertiesfileLocation) throws IOException {
		InputStream configurationInputStream = null;
		if (Files.isRegularFile(propertiesfileLocation)) {
			// Try to load from disk
			configurationInputStream = Files.newInputStream(propertiesfileLocation);
		} else if (getClass().getResourceAsStream(propertiesfileLocation.toString()) != null) {
			// Try to load from classpath
			configurationInputStream = getClass().getResourceAsStream(propertiesfileLocation.toString());
		}
		return configurationInputStream;
	}

	/**
	 * Gets the rss location.
	 *
	 * @return the rss location
	 */
	public Path getRssLocation() {
		final String rssLocation = configuration.getProperty("rss.location", "rss.xml");
		return FileSystems.getDefault().getPath(rssLocation);
	}

	/**
	 * Gets the data file location.
	 *
	 * @return the data file location
	 */
	public Path getDataFileLocation() {
		final String dataFileLocation = configuration.getProperty("ffp.data.file.location", "ffp-data.xml");
		return FileSystems.getDefault().getPath(dataFileLocation);
	}

	/**
	 * Gets the rss url.
	 *
	 * @return the rss url
	 */
	public String getPublicUrlRss() {
		return configuration.getProperty("public.url.rss", "http://unknown/${file.name}");
	}

	/**
	 * Checks if is enable bittorent.
	 *
	 * @return the boolean
	 */
	public Boolean isEnableBittorent() {
		return Boolean.valueOf(configuration.getProperty("ffp.enable.bittorrent", "false"));
	}

	/**
	 * Checks if is enable http.
	 *
	 * @return the boolean
	 */
	public Boolean isEnableHttp() {
		return Boolean.valueOf(configuration.getProperty("ffp.enable.http", "true"));
	}

	/**
	 * Gets the repeat interval.
	 *
	 * @return the repeat interval
	 */
	public Long getRepeatInterval() {
		return Long.valueOf(configuration.getProperty("folder.scan.interval.seconds", "600"));
	}

	/**
	 * Gets the file change cooldown.
	 *
	 * @return the file change cooldown
	 */
	public Long getFileChangeCooldown() {
		return Long.valueOf(configuration.getProperty("folder.scan.file.change.cooldown.seconds", "3600"));
	}

	/**
	 * Gets the folder to watch.
	 *
	 * @return the folder to watch
	 */
	public List<Path> getFoldersToWatch() {
		final List<Path> foldersToWatch = new ArrayList<>();
		final String key = "folders.to.watch.";
		int i = 1;
		while (configuration.containsKey(key + i)) {
			final String folderLocation = configuration.getProperty(key + i, null);
			if (folderLocation != null && folderLocation.trim().length() != 0) {
				final Path folder = Paths.get(folderLocation);
				foldersToWatch.add(folder);
			}
			i += 1;
		}
		return foldersToWatch;
	}

	/**
	 * Gets the torrent tracker port.
	 *
	 * @return the torrent tracker port
	 */
	public Integer getTorrentTrackerPort() {
		return Integer.valueOf(configuration.getProperty("torrent.tracker.port", "6969"));
	}

	/**
	 * Gets the torrent tracker ip.
	 *
	 * @return the torrent tracker ip
	 */
	public String getTorrentTrackerIp() {
		return configuration.getProperty("torrent.tracker.ip", "127.0.0.1");
	}

	/**
	 * Gets the public url tracker announce.
	 *
	 * @return the public url tracker announce
	 */
	public String getPublicUrlTrackerAnnounce() {
		return configuration.getProperty("public.url.tracker.announce", "http://unkown:6969/announce");
	}

	/**
	 * Gets the public url torrent.
	 *
	 * @return the public url torrent
	 */
	public String getPublicUrlTorrent() {
		return configuration.getProperty("public.url.torrent", "http://unknown/${file.name}");
	}

	/**
	 * Gets the torrent file folder.
	 *
	 * @return the torrent file folder
	 */
	public String getTorrentFileFolder() {
		return configuration.getProperty("torrent.file.folder", "www-data");
	}

	/**
	 * Gets the torrent extension.
	 *
	 * @return the torrent extension
	 */
	public String getTorrentExtension() {
		return configuration.getProperty("torrent.extension", ".torrent");
	}
}
