/*
 *
 */
package eu.gaki.ffp.service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpItem;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(RssService.class);

	/** The main rss. */
	private String mainRss;

	/** The item rss. */
	private String itemRss;

	/** The rss file items. */
	private Collection<RssFileItem> rssFileItems;

	/** The rss date formateur. */
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z",
			Locale.ENGLISH);

	/** The torrent url template. */
	private final String torrentUrlTemplate;

	/** The rss file. */
	private final Path rssFile;

	/** The rss url template. */
	private final String rssUrlTemplate;

	/**
	 * Instantiates a new torrent rss.
	 *
	 * @param configService
	 *            the config service
	 */
	public RssService(final ConfigService configService) {
		torrentUrlTemplate = configService.getPublicUrlTorrent();
		rssFile = configService.getRssLocation();
		rssUrlTemplate = configService.getPublicUrlRss();
		// Get template files
		try (InputStream mainRssStream = RssService.class.getResourceAsStream(MAIN_RSS_TEMPLATE);
				InputStream itemRssStream = RssService.class.getResourceAsStream(ITEM_RSS_TEMPLATE)) {
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
	 * @param ffpItems
	 *            the ffp items
	 * @return the file
	 */
	public Path generateRss(final Collection<FfpItem> ffpItems) {
		final Collection<RssFileItem> rssFileItems = getRssItemList(ffpItems);

		// If something change rewrite the RSS file
		if (isRssItemsChanged(rssFileItems)) {
			this.rssFileItems = rssFileItems;

			final String rssUrl = rssUrlTemplate.replace("${file.name}", rssFile.getFileName().toString());
			// Get torrent file URL

			try (FileChannel rssFileChanel = FileChannel.open(rssFile, StandardOpenOption.WRITE,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);) {

				final StringBuilder items = new StringBuilder();
				for (final RssFileItem rssFileItem : rssFileItems) {
					String item;
					item = itemRss.replace("${file.name}", rssFileItem.getName());
					item = item.replace("${file.url}", rssFileItem.getUrl());
					item = item.replace("${file.size}", Long.toString(rssFileItem.getSize()));
					item = item.replace("${file.date}", simpleDateFormat.format(rssFileItem.getDate()));
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

	/**
	 * Gets the rss item list.
	 *
	 * @param items
	 *            the items
	 * @return the rss item list
	 */
	private Set<RssFileItem> getRssItemList(final Collection<FfpItem> items) {
		final Set<RssFileItem> rssFileItems = new HashSet<>();
		// Publish RSS file with tracked torrent files
		items.forEach(item -> {
			final RssFileItem rssFileItem = new RssFileItem();
			// Rss link name
			final String fileName = item.getFfpFiles().get(0).getPath().getFileName().toString();
			rssFileItem.setName(fileName);
			// Rss file URL
			final Path torrentFile = item.getTorrentPath();
			final String name = torrentFile.getFileName().toString();
			String nameUrl;
			try {
				nameUrl = URLEncoder.encode(name, "UTF-8").replace("+", "%20");
			} catch (final UnsupportedEncodingException e) {
				LOGGER.error("Error when URL encode the file name. Fallback without URL encode", e);
				nameUrl = name;
			}
			rssFileItem.setUrl(torrentUrlTemplate.replace("${file.name}", nameUrl));
			// Rss file date
			FileTime lastModifiedTime;
			try {
				lastModifiedTime = Files.getLastModifiedTime(torrentFile);
				rssFileItem.setDate(new Date(lastModifiedTime.toMillis()));
			} catch (final Exception e) {
				LOGGER.error("Cannot determine the modification date of " + torrentFile, e);
				rssFileItem.setDate(new Date());
			}
			// Rss file size
			try {
				rssFileItem.setSize(Files.size(torrentFile));
			} catch (final Exception e) {
				LOGGER.error("Cannot compute the size of " + torrentFile, e);
				rssFileItem.setSize(0L);
			}
			rssFileItems.add(rssFileItem);
		});
		return rssFileItems;
	}

}
