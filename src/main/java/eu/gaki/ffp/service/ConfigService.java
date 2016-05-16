/*
 *
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public String getRssLocation() {
	return configuration.getProperty("rss.location", "rss.xml");
    }

    /**
     * Gets the rss url.
     *
     * @return the rss url
     */
    public String getRssUrl() {
	return configuration.getProperty("rss.url", "http://unknown/${file.name}");
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
}
