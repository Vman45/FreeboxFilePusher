/**
 *
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: Auto-generated Javadoc
/**
 * Initialize and provide acces to Service layer.
 *
 * @author Pilou
 */
public class ServiceProvider {

	/** The config service. */
	private final ConfigService configService;

	/** The rss file generator. */
	private final RssService rssFileGenerator;

	/** The dao service. */
	private final DaoService daoService;

	/** The checksum service. */
	private final ChecksumService checksumService;

	/** The file service. */
	private final FileService fileService;

	/** The item service. */
	private final ItemService itemService;

	/** The bt service. */
	private final BtService btService;

	/**
	 * Instantiates a new service provider.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred during reading of
	 *             configuration file.
	 */
	public ServiceProvider() throws IOException {
		this("freeboxFilePusher.properties");
	}

	/**
	 * Instantiates a new service provider.
	 *
	 * @param configFilePath
	 *            the config file path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred during reading of
	 *             configuration file.
	 */
	public ServiceProvider(final String configFilePath) throws IOException {
		// Load configuration file
		final Path configPath = Paths.get(configFilePath);

		// Initialize Services
		configService = new ConfigService(configPath);
		rssFileGenerator = new RssService(configService);
		daoService = new DaoService(configService);
		checksumService = new ChecksumService(configService);
		fileService = new FileService();
		itemService = new ItemService(fileService);
		btService = new BtService(configService, daoService);
	}

	/**
	 * Gets the config service.
	 *
	 * @return the configService
	 */
	public ConfigService getConfigService() {
		return configService;
	}

	/**
	 * Gets the rss file generator.
	 *
	 * @return the rssFileGenerator
	 */
	public RssService getRssFileGenerator() {
		return rssFileGenerator;
	}

	/**
	 * Gets the dao service.
	 *
	 * @return the daoService
	 */
	public DaoService getDaoService() {
		return daoService;
	}

	/**
	 * Gets the checksum service.
	 *
	 * @return the checksumService
	 */
	public ChecksumService getChecksumService() {
		return checksumService;
	}

	/**
	 * Gets the file service.
	 *
	 * @return the fileService
	 */
	public FileService getFileService() {
		return fileService;
	}

	/**
	 * Gets the item service.
	 *
	 * @return the itemService
	 */
	public ItemService getItemService() {
		return itemService;
	}

	/**
	 * Gets the bt service.
	 *
	 * @return the btService
	 */
	public BtService getBtService() {
		return btService;
	}

}
