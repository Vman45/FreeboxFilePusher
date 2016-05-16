/*
 *
 */
package eu.gaki.ffp.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Main database object.
 */
public class FilePusher {

    /**
     * List of FfpItem used by the system.
     */
    private List<FfpItem> items = new ArrayList<>();

    /**
     * Sets the item watched.
     *
     * @param items
     *            the new item watched
     */
    public void setItems(final List<FfpItem> items) {
	this.items = items;
    }

    /**
     * Gets the item watched.
     *
     * @return the item watched
     */
    public List<FfpItem> getItems() {
	return this.items;
    }

    /**
     * Adds the ffp item.
     *
     * @param item
     *            the item
     */
    public void addFfpItem(final FfpItem item) {
	items.add(item);
    }

}
