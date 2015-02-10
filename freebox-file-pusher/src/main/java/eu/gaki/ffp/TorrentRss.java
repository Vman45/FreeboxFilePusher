package eu.gaki.ffp;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.turn.ttorrent.tracker.TrackedTorrent;

/**
 * The Class TorrentRss.
 */
public class TorrentRss {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TorrentRss.class);

    /** The Constant DATE_PARSER: Mon, 22 Jul 2013 00:12:38 +0200 */
    private static final DateFormat DATE_PARSER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    /** The main rss. */
    private String mainRss;

    /** The item rss. */
    private String itemRss;

    /**
     * Instantiates a new torrent rss.
     */
    public TorrentRss() {
	try (InputStream mainRssStream = TorrentRss.class.getResourceAsStream("mainRss.template");
		InputStream itemRssStream = TorrentRss.class.getResourceAsStream("itemRss.template")) {
	    mainRss = IOUtils.toString(mainRssStream);
	    itemRss = IOUtils.toString(itemRssStream);
	} catch (final Exception e) {
	    LOGGER.error("Cannot read RSS template files:" + e.getMessage(), e);
	}
    }

    /**
     * Generate rss.
     *
     * @param configuration
     *            the configuration
     * @param torrentFiles
     *            the torrent files
     * @return the file
     */
    public File generateRss(final Properties configuration, final Collection<TrackedTorrent> torrentFiles) {

	// Get RSS file location
	final String rssLocation = configuration.getProperty("rss.location", "rss.xml");
	final File rssFile = new File(rssLocation);
	// Get RSS file URL
	final String rssUrlTemplate = configuration.getProperty("rss.url", "http://unknown/${file.name}");
	final String rssUrl = rssUrlTemplate.replace("${file.name}", FilenameUtils.getName(rssLocation));
	// Get torrent file URL
	final String torrentUrlTemplate = configuration.getProperty("torrent.url", "http://unknown/${file.name}");
	try (Writer writer = new FileWriter(rssFile)) {

	    final StringBuilder items = new StringBuilder();
	    for (final TrackedTorrent torrent : torrentFiles) {
		String item;
		final String name = torrent.getName();
		String nameUrl;
		try {
		    nameUrl = URLEncoder.encode(name, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
		    LOGGER.error("Error when URL encode the file name. Fallback without URL encode", e);
		    nameUrl = name;
		}
		item = itemRss.replace("${file.name}", name);
		item = item.replace("${file.url}",
			torrentUrlTemplate.replace("${file.name}",
				nameUrl + configuration.getProperty("torrent.extention", ".torrent")));
		item = item.replace("${file.size}", Long.toString(torrent.getSize()));
		final File torrentFile = torrent.getSeederClient().getTorrentFile();
		final FileTime lastModifiedTime = Files.getLastModifiedTime(torrentFile.toPath());
		item = item.replace("${file.date}", DATE_PARSER.format(new Date(lastModifiedTime.toMillis())));
		items.append(item);
	    }

	    String rss;
	    rss = mainRss.replace("${rss.url}", rssUrl);
	    rss = rss.replace("${items}", items.toString());

	    writer.write(rss);
	} catch (final Exception e) {
	    LOGGER.error("Cannot write rss file:" + e.getMessage(), e);
	}

	return rssFile;
    }

}
