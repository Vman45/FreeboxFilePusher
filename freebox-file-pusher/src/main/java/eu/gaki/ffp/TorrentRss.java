package eu.gaki.ffp;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedOutput;

public class TorrentRss {

// Rss exemple
//	<rss xmlns:atom="http://www.w3.org/2005/Atom" version="2.0">
//		<channel>
//			<title>Movies > Action - Mininova</title>
//			<link>http://www.mininova.org/</link>
//			<description>Latest Mininova torrents</description>
//			<language>en-us</language>
//			<atom:link href="http://www.mininova.org/rss.xml?sub=1" rel="self" type="application/rss+xml"/>
//			<item>
//				<title>Fat Man & Car</title>
//				<guid isPermaLink="true">http://www.mininova.org/tor/13275142</guid>
//				<pubDate>Thu, 22 May 2014 12:43:15 +0200</pubDate>
//				<category>Movies</category>
//				<link>http://www.mininova.org/tor/13275142</link>
//				<enclosure url="http://www.mininova.org/get/13275142" length="1275879" type="application/x-bittorrent"/>
//				<description>
//					<![CDATA[
//					Category: <a href="http://www.mininova.org/cat/4">Movies</a><br /> Subcategory: <a href="http://www.mininova.org/sub/1">Action</a><br /> Size: 1.22&nbsp;megabyte<br /> Ratio: 2 seeds, 1 leechers<br /> Language: Unknown<br /> Uploaded by: <a href="http://www.mininova.org/user/enty3way">enty3way</a>
//					]]>
//				</description>
//			</item>
//		</channel>
//	</rss>
	
	private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd");
	
	public File generateRss(File directory, Collection<TorrentFile> torrentFiles) {
		
		String[] args = new String[]{"atom_1.0","freeboxFilePusherRss.xml"};
		
		 boolean ok = false;
	        if (args.length==2) {
	            try {
	                String feedType = args[0];
	                String fileName = args[1];

	                SyndFeed feed = new SyndFeedImpl();
	                feed.setFeedType(feedType);

	                feed.setTitle("freeboxFilePusherRss");
	                feed.setLink("http://rome.dev.java.net");
	                feed.setDescription("freeboxFilePusherRss");

	                List entries = new ArrayList();
	                SyndEntry entry;
	                SyndContent description;

	                entry = new SyndEntryImpl();
	                entry.setTitle("ROME v1.0");
	                entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome01");
	                entry.setPublishedDate(DATE_PARSER.parse("2004-06-08"));
	                description = new SyndContentImpl();
	                description.setType("text/plain");
	                description.setValue("Initial release of ROME");
	                entry.setDescription(description);
	                entries.add(entry);

	                entry = new SyndEntryImpl();
	                entry.setTitle("ROME v2.0");
	                entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome02");
	                entry.setPublishedDate(DATE_PARSER.parse("2004-06-16"));
	                description = new SyndContentImpl();
	                description.setType("text/plain");
	                description.setValue("Bug fixes, minor API changes and some new features");
	                entry.setDescription(description);
	                entries.add(entry);

	                entry = new SyndEntryImpl();
	                entry.setTitle("ROME v3.0");
	                entry.setLink("http://wiki.java.net/bin/view/Javawsxml/Rome03");
	                entry.setPublishedDate(DATE_PARSER.parse("2004-07-27"));
	                description = new SyndContentImpl();
	                description.setType("text/html");
	                description.setValue("<p>More Bug fixes, mor API changes, some new features and some Unit testing</p>"+
	                                     "<p>For details check the <a href=\"https://rometools.jira.com/wiki/display/ROME/Change+Log#ChangeLog-Changesmadefromv0.3tov0.4\">Changes Log</a></p>");
	                entry.setDescription(description);
	                entries.add(entry);

	                feed.setEntries(entries);

	                Writer writer = new FileWriter(fileName);
	                SyndFeedOutput output = new SyndFeedOutput();
	                output.output(feed,writer);
	                writer.close();

	                System.out.println("The feed has been written to the file ["+fileName+"]");

	                ok = true;
	            }
	            catch (Exception ex) {
	                ex.printStackTrace();
	                System.out.println("ERROR: "+ex.getMessage());
	            }
	        }

	        if (!ok) {
	            System.out.println();
	            System.out.println("FeedWriter creates a RSS/Atom feed and writes it to a file.");
	            System.out.println("The first parameter must be the syndication format for the feed");
	            System.out.println("  (rss_0.90, rss_0.91, rss_0.92, rss_0.93, rss_0.94, rss_1.0 rss_2.0 or atom_0.3)");
	            System.out.println("The second parameter must be the file name for the feed");
	            System.out.println();
	        }
		
		return null;
	}
	
}
