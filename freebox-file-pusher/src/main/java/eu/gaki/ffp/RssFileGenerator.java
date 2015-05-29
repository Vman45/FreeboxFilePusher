package eu.gaki.ffp;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class TorrentRss.
 */
public class RssFileGenerator {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(RssFileGenerator.class);

	/** The main rss. */
	private String mainRss;

	/** The item rss. */
	private String itemRss;

	/** The rss file items. */
	private Collection<RssFileItem> rssFileItems;

	/**
	 * Instantiates a new torrent rss.
	 */
	public RssFileGenerator() {
		try (InputStream mainRssStream = RssFileGenerator.class.getResourceAsStream("mainRss.template");
				InputStream itemRssStream = RssFileGenerator.class.getResourceAsStream("itemRss.template")) {
			mainRss = IOUtils.toString(mainRssStream);
			itemRss = IOUtils.toString(itemRssStream);
		} catch (final Exception e) {
			LOGGER.error("Cannot read RSS template files: {}", e.getMessage(), e);
		}
	}

	/**
	 * Generate RSS file if some difference are found with the previous
	 * generated file.
	 *
	 * @param configuration
	 *            the configuration
	 * @param rssFileItems
	 *            the torrent files
	 * @return the file
	 */
	public Path generateRss(final Properties configuration, final Collection<RssFileItem> rssFileItems) {

		// Get RSS file location
		final String rssLocation = configuration.getProperty("rss.location", "rss.xml");
		final Path rssFile = FileSystems.getDefault().getPath(rssLocation);

		// If something change rewrite the RSS file
		if (isRssItemsChanged(rssFileItems)) {
			this.rssFileItems = rssFileItems;
			// Get RSS file URL
			final String rssUrlTemplate = configuration.getProperty("rss.url", "http://unknown/${file.name}");
			final String rssUrl = rssUrlTemplate.replace("${file.name}", FilenameUtils.getName(rssLocation));
			// Get torrent file URL

			try (FileChannel rssFileChanel = FileChannel.open(rssFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);) {

				final StringBuilder items = new StringBuilder();
				for (final RssFileItem rssFileItem : rssFileItems) {
					String item;
					item = itemRss.replace("${file.name}", rssFileItem.getName());
					item = item.replace("${file.url}", rssFileItem.getUrl());
					item = item.replace("${file.size}", Long.toString(rssFileItem.getSize()));
					item = item.replace("${file.date}", new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(rssFileItem.getDate()));
					items.append(item);
				}

				String rss = mainRss.replace("${rss.url}", rssUrl);
				rss = rss.replace("${items}", items.toString());
				final ByteBuffer wrap = ByteBuffer.wrap(rss.getBytes());
				rssFileChanel.write(wrap);
				LOGGER.trace("Write RSS file {}", rssLocation);
			} catch (final Exception e) {
				LOGGER.error("Cannot write rss file: {}", e.getMessage(), e);
			}
		}
		return rssFile;
	}

	/**
	 * Checks if is rss items changed.
	 *
	 * @param rssFileItems
	 *            the rss file items
	 * @return true, if is rss items changed
	 */
	private boolean isRssItemsChanged(final Collection<RssFileItem> rssFileItems) {
		boolean rewriteRss = false;
		if (this.rssFileItems == null) {
			this.rssFileItems = rssFileItems;
			rewriteRss = true;
		}
		final int size = this.rssFileItems.size();
		final int newSize = rssFileItems.size();
		if (size != newSize) {
			rewriteRss = true;
		} else {
			for (final RssFileItem rssFileItem : rssFileItems) {
				if (!this.rssFileItems.contains(rssFileItem)) {
					rewriteRss = true;
					break;
				}
			}
		}
		return rewriteRss;
	}

}
