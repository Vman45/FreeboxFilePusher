/*
 *
 */
package eu.gaki.ffp.service;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.RssFileItem;

/**
 * Generate an RSS file.
 */
public class RssService {

	/** The Constant ITEM_RSS_TEMPLATE. */
	private static final String ITEM_RSS_TEMPLATE = "itemRss.template";

	/** The Constant MAIN_RSS_TEMPLATE. */
	private static final String MAIN_RSS_TEMPLATE = "mainRss.template";

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(RssService.class);

	/** The main rss. */
	private String mainRss;

	/** The item rss. */
	private String itemRss;

	/** The rss file items. */
	private Collection<RssFileItem> rssFileItems;

	/** The rss date formateur. */
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

	/** The config service. */
	private final ConfigService configService;

	/**
	 * Instantiates a new torrent rss.
	 *
	 * @param configService
	 *            the config service
	 */
	public RssService(final ConfigService configService) {
		this.configService = configService;
		// Get template files
		try (InputStream mainRssStream = RssService.class
				.getResourceAsStream(MAIN_RSS_TEMPLATE);
				InputStream itemRssStream = RssService.class
						.getResourceAsStream(ITEM_RSS_TEMPLATE)) {
			mainRss = IOUtils.toString(mainRssStream);
			itemRss = IOUtils.toString(itemRssStream);
		} catch (final Exception e) {
			LOGGER.error("Cannot read RSS template files: {}", e.getMessage(),
					e);
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
	public Path generateRss(final Collection<RssFileItem> rssFileItems) {

		// Get RSS file location
		final Path rssFile = configService.getRssLocation();

		// If something change rewrite the RSS file
		if (isRssItemsChanged(rssFileItems)) {
			this.rssFileItems = rssFileItems;
			// Get RSS file URL
			final String rssUrlTemplate = configService.getRssUrl();

			final String rssUrl = rssUrlTemplate.replace("${file.name}",
					rssFile.getFileName().toString());
			// Get torrent file URL

			try (FileChannel rssFileChanel = FileChannel.open(rssFile,
					StandardOpenOption.WRITE, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);) {

				final StringBuilder items = new StringBuilder();
				for (final RssFileItem rssFileItem : rssFileItems) {
					String item;
					item = itemRss.replace("${file.name}",
							rssFileItem.getName());
					item = item.replace("${file.url}", rssFileItem.getUrl());
					item = item.replace("${file.size}",
							Long.toString(rssFileItem.getSize()));
					item = item.replace("${file.date}",
							simpleDateFormat.format(rssFileItem.getDate()));
					items.append(item);
				}

				String rss = mainRss.replace("${rss.url}", rssUrl);
				rss = rss.replace("${items}", items.toString());
				final ByteBuffer wrap = ByteBuffer.wrap(rss.getBytes());
				rssFileChanel.write(wrap);
				LOGGER.trace("Write RSS file {}", rssFile);
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
