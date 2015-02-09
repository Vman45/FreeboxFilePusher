package eu.gaki.ffp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.turn.ttorrent.tracker.TrackedTorrent;

/**
 * The Class TorrentRss.
 */
public class TorrentRss {

    /** The Constant DATE_PARSER. */
    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd");

    /** The main rss. */
    private String mainRss;

    /** The item rss. */
    private String itemRss;

    /**
     * Instantiates a new torrent rss.
     */
    public TorrentRss() {
	InputStream mainRssStream = null;
	InputStream itemRssStream = null;
	try {
	    mainRssStream = TorrentRss.class.getResourceAsStream("/mainRss.template");
	    mainRss = IOUtils.toString(mainRssStream);
	    itemRssStream = TorrentRss.class.getResourceAsStream("/itemRss.template");
	    itemRss = IOUtils.toString(itemRssStream);
	} catch (final Exception ex) {
	    ex.printStackTrace();
	} finally {
	    try {
		if (mainRssStream != null) {
		    mainRssStream.close();
		}
		if (itemRssStream != null) {
		    itemRssStream.close();
		}
	    } catch (final IOException e) {
		e.printStackTrace();
	    }
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

	Writer writer = null;
	final String rssLocation = configuration.getProperty("rss.location", "http://unknown/rss.xml");
	final File rssFile = new File(rssLocation);
	final String torrentUrl = configuration.getProperty("torrent.url", "http://unknown/${file.name}");
	final String rssUrl = torrentUrl.replace("${file.name}", "rss.xml");
	try {

	    final StringBuilder items = new StringBuilder();
	    for (final TrackedTorrent torrent : torrentFiles) {
		String item;
		item = itemRss.replace("${file.name}", torrent.getName());
		item = item.replace("${file.url}", torrentUrl.replace("${file.name}", torrent.getName() + ".torrent"));
		item = item.replace("${file.size}", Long.toString(torrent.getSize()));
		items.append(item);
	    }

	    String rss;
	    rss = mainRss.replace("${rss.url}", rssUrl);
	    rss = rss.replace("${items}", items.toString());

	    writer = new FileWriter(rssFile);
	    writer.write(rss);
	}
	catch (final Exception ex) {
	    ex.printStackTrace();
	} finally {
	    try {
		if (writer != null) {
		    writer.close();
		}
	    } catch (final IOException e) {
		e.printStackTrace();
	    }
	}

	return rssFile;
    }

}
