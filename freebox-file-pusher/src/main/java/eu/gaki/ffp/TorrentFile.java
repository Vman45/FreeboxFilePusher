package eu.gaki.ffp;

import java.net.URL;

public class TorrentFile {
	private String title;
	private URL link;
	private String description;
	private String category;
	private int length;
	private URL torrentFile;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public URL getLink() {
		return link;
	}
	public void setLink(URL link) {
		this.link = link;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public URL getTorrentFile() {
		return torrentFile;
	}
	public void setTorrentFile(URL torrentFile) {
		this.torrentFile = torrentFile;
	}
	
	
}
