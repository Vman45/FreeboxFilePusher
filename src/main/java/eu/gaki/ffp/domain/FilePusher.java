/*
 *
 */
package eu.gaki.ffp.domain;

import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * Objet principal.
 */
public class FilePusher {

    /**
     * List of filesList to watch by the system.
     */
    private List<FfpItem> itemWatched = new ArrayList<>();

    /**
     * Sets the item watched.
     *
     * @param itemWatched
     *            the new item watched
     */
    public void setItemWatched(final List<FfpItem> itemWatched) {
	this.itemWatched = itemWatched;
    }

    /**
     * Gets the item watched.
     *
     * @return the item watched
     */
    public List<FfpItem> getItemWatched() {
	return this.itemWatched;
    }

    /**
     * Adds the ffp item.
     *
     * @param item
     *            the item
     */
    public void addFfpItem(final FfpItem item) {
	itemWatched.add(item);
    }

}
