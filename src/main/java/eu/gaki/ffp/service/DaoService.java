/*
 *
 */
package eu.gaki.ffp.service;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.FilePusher;
import eu.gaki.ffp.domain.StatusEnum;

/**
 * He we just serialize/deserialize main objet domain.
 */
public class DaoService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(DaoService.class);

	/** The Constant DATA_FILE. */
	private final Path dataFile;
	/**
	 * The main objet domain.
	 */
	private FilePusher filePusher;

	/**
	 * Instantiates a new dao service.
	 *
	 * @param configService
	 *            the config service
	 */
	public DaoService(final ConfigService configService) {
		dataFile = configService.getDataFileLocation();
		// Load the data
		loadDataFromFile();
	}

	/**
	 * Load data from file.
	 */
	private synchronized void loadDataFromFile() {
		if (Files.exists(dataFile)) {
			try (InputStream is = Files.newInputStream(dataFile);
					BufferedInputStream bis = new BufferedInputStream(is);) {
				final XMLDecoder d = new XMLDecoder(bis);
				filePusher = (FilePusher) d.readObject();
				d.close();
			} catch (final IOException e) {
				LOGGER.error("Dao load error", e);
				LOGGER.info("Dao load new Data");
				filePusher = new FilePusher();
			}
		} else {
			LOGGER.info("Dao load new Data");
			filePusher = new FilePusher();
		}
	}

	/**
	 * Gets the.
	 *
	 * @return the file pusher
	 */
	public FilePusher get() {
		if (filePusher == null) {
			// Load the data
			loadDataFromFile();
		}
		return filePusher;
	}

	/**
	 * Save.
	 */
	public synchronized void save() {
		try {
			final XMLEncoder e = new XMLEncoder(new BufferedOutputStream(Files.newOutputStream(dataFile)));
			e.writeObject(filePusher);
			e.close();
		} catch (final IOException e) {
			LOGGER.error("Dao save error", e);
		}
	}

	/**
	 * Clear.
	 */
	public synchronized void clear() {
		try {
			Files.deleteIfExists(dataFile);
			filePusher = new FilePusher();
		} catch (final IOException e) {
			LOGGER.error("Dao clear error", e);
		}
	}

	/**
	 * Search an {@link FfpFile} by {@link URI}.
	 *
	 * @param uri
	 *            The searched {@link URI}
	 * @return The FfpItem which contain the {@link URI}
	 */
	public List<FfpItem> contains(final URI uri) {
		final List<FfpItem> result = get().getItems().parallelStream().filter(p -> !p.contains(uri).isEmpty())
				.collect(Collectors.toList());
		return result;
	}

	/**
	 * Return the list of items which is in a specified status.
	 *
	 * @param status
	 *            the status to be get or null to get all
	 * @return the list of {@link FfpItem}
	 */
	public List<FfpItem> getByStatus(final StatusEnum status) {
		return get().getItems().parallelStream().filter(p -> {
			boolean result = true;
			if (status != null) {
				result = status.equals(p.getStatus());
			}
			return result;
		}).collect(Collectors.toList());
	}

}
