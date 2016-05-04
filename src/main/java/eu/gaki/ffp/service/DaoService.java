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

import eu.gaki.ffp.domain.FilePusher;

/**
 * He we just serialize/deserialize main objet domain
 */
public class DaoService {

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
		final XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream("Test.xml")));
		filePusher = (FilePusher) d.readObject();
		d.close();
	    }
	} catch (final FileNotFoundException e) {
	    e.printStackTrace();
	    filePusher = new FilePusher();
	}
	return filePusher;
    }

    public void save() {
	try {
	    final XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("Test.xml")));
	    e.writeObject(filePusher);
	    e.close();
	} catch (final FileNotFoundException e) {
	    e.printStackTrace();
	}
    }

}
