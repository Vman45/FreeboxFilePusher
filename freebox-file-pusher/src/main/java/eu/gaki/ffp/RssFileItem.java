package eu.gaki.ffp;

import java.util.Date;
import java.util.Objects;

/**
 * The Class RssFileItem.
 */
public class RssFileItem {
	
	/** The name. */
	private String name;
	
	/** The url. */
	private String url;
	
	/** The date. */
	private Date date;
	
	/** The size. */
	private Long size;
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the url.
	 *
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * Sets the url.
	 *
	 * @param url the new url
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * Gets the date.
	 *
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}
	
	/**
	 * Sets the date.
	 *
	 * @param date the new date
	 */
	public void setDate(Date date) {
		this.date = date;
	}
	
	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	public Long getSize() {
		return size;
	}
	
	/**
	 * Sets the size.
	 *
	 * @param size the new size
	 */
	public void setSize(Long size) {
		this.size = size;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof RssFileItem) {
			RssFileItem r = (RssFileItem) obj;
			result = Objects.equals(name, r.name) && Objects.equals(url, r.url) && Objects.equals(date, r.date) && Objects.equals(size, r.size);
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hash(name, url, date, size);
	}

}
