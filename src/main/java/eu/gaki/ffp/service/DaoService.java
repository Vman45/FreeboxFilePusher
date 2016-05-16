/*
 *
 */
package eu.gaki.ffp.service;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.FilePusher;
import eu.gaki.ffp.domain.StatusEnum;

/**
 * He we just serialize/deserialize main objet domain
 */
public class DaoService {

    /** The Constant DATA_FILE. */
    private static final String DATA_FILE = "Test.xml";
    /**
     * The main objet domain.
     */
    private FilePusher filePusher;

    /**
     * Gets the.
     *
     * @return the file pusher
     */
    public FilePusher get() {
	try {
	    if (filePusher == null) {
		final XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(DATA_FILE)));
		filePusher = (FilePusher) d.readObject();
		d.close();
	    }
	} catch (final FileNotFoundException e) {
	    filePusher = new FilePusher();
	}
	return filePusher;
    }

    /**
     * Save.
     */
    public void save() {
	try {
	    final XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(DATA_FILE)));
	    e.writeObject(filePusher);
	    e.close();
	} catch (final FileNotFoundException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Clear.
     */
    public void clear() {
	try {
	    Files.deleteIfExists(Paths.get(DATA_FILE));
	} catch (final IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    /**
     * Search an {@link FfpFile} by {@link URI}
     *
     * @param uri
     *            The searched {@link URI}
     * @return The FfpItem which contain the {@link URI}
     */
    public List<FfpItem> contains(final URI uri) {
	final List<FfpItem> result = get().getItems().parallelStream().filter(p -> !p.contains(uri).isEmpty())
		.collect(Collectors.toList());
	return result;
    }

    /**
     * Return the list of items which is in a specified status.
     *
     * @param status
     *            the status to be get or null to get all
     * @return the list of {@link FfpItem}
     */
    public List<FfpItem> getByStatus(final StatusEnum status) {
	return get().getItems().parallelStream().filter(p -> {
	    boolean result = true;
	    if (status != null) {
		result = status.equals(p.getStatus());
	    }
	    return result;
	}).collect(Collectors.toList());
    }

}
