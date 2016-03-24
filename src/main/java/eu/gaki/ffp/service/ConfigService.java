package eu.gaki.ffp.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigService {
    
	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
    
    
    /** The configuration. */
	private Properties configuration;
	
    public ConfigService(Path configFile) throws IOException {
        loadConfigurationFile(configFile);
    }
    
	/**
	 * Load configuration file.
	 *
	 * @param args
	 *            the args
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void loadConfigurationFile(final Path fileLocation) throws IOException {
		configuration = new Properties();
		Path propertiesfileLocation = Paths.get("freeboxFilePusher.properties");
		if (fileLocation != null) {
			propertiesfileLocation = fileLocation;
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
	 * @throws FileNotFoundException
	 *             the file not found exception
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
	
	public String getRssLocation() {
	   return configuration.getProperty("rss.location", "rss.xml");
	}
	
	public String getRssUrl() {
	   return configuration.getProperty("rss.url", "http://unknown/${file.name}");
    }
}
