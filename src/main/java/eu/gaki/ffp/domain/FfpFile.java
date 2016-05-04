/*
 *
 */
package eu.gaki.ffp.domain;

import java.beans.Transient;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * File.
 */
public class FfpFile {

    /** The file path. */
    private Path path = null;

    /** The path uri. */
    private URI pathUri = null;

    /**
     * The date when we compute the checksome adler32 for this file.
     */
    private LocalDateTime adler32Date = null;

    /**
     * Map for store checksome : Bytes number => Checksome value.<br>
     * 20000 => Checksome for byte 0 to byte 20000<br>
     * 40000 => Checksome for byte 20001 to byte 40000<br>
     */
    private Map<Long, Long> adler32 = new HashMap<>();

    /**
     * Size of file.
     */
    private Long size;

    /**
     * Sets the adler32 date.
     *
     * @param adler32Date
     *            the new adler32 date
     */
    public void setAdler32Date(final LocalDateTime adler32Date) {
	this.adler32Date = adler32Date;
    }

    /**
     * Gets the adler32 date.
     *
     * @return the adler32 date
     */
    public LocalDateTime getAdler32Date() {
	return this.adler32Date;
    }

    /**
     * Sets the path.
     *
     * @param pathUri
     *            the new uri
     */
    public void setPathUri(final URI pathUri) {
	this.pathUri = pathUri;
	if (pathUri != null) {
	    this.path = Paths.get(pathUri);
	}
    }

    /**
     * Gets the path uri.
     *
     * @return the path uri
     */
    public URI getPathUri() {
	return this.pathUri;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    @Transient
    public Path getPath() {
	return this.path;
    }

    /**
     * Gets the adler32.
     *
     * @return the adler32
     */
    public Map<Long, Long> getAdler32() {
	return this.adler32;
    }

    /**
     * Sets the adler32.
     *
     * @param adler32
     *            the adler32
     */
    public void setAdler32(final Map<Long, Long> adler32) {
	this.adler32 = adler32;
    }

    /**
     * Sets the size.
     *
     * @param size
     *            the new size
     */
    public void setSize(final Long size) {
	this.size = size;
    }

    /**
     * Gets the size.
     *
     * @return the size
     */
    public Long getSize() {
	return this.size;
    }

}
