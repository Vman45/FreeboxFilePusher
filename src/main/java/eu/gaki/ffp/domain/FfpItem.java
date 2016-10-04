/*
 *
 */
package eu.gaki.ffp.domain;

import java.beans.Transient;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

// TODO: Auto-generated Javadoc
/**
 * Represent a set of FfpFile to be sended in the same time.
 */
public class FfpItem {

	/** List of files. */
	private List<FfpFile> ffpFiles = new ArrayList<>();

	/**
	 * Status of this pack of ffpFile.
	 */
	private StatusEnum status;

	// /** TRANSIENT : The bt. */
	// private Torrent torrent = null;

	/** TRANSIENT : The bt file path. */
	private Path torrentPath = null;

	/** The bt path uri. */
	private URI torrentPathUri = null;

	/**
	 * Sets the ffp files.
	 *
	 * @param ffpFiles
	 *            the new ffp files
	 */
	public void setFfpFiles(final List<FfpFile> ffpFiles) {
		this.ffpFiles = ffpFiles;
	}

	/**
	 * Gets the ffp files.
	 *
	 * @return the ffp files
	 */
	public List<FfpFile> getFfpFiles() {
		return this.ffpFiles;
	}

	/**
	 * Adds the file.
	 *
	 * @param file
	 *            the file
	 */
	public void addFile(final FfpFile file) {
		ffpFiles.add(file);
	}

	/**
	 * Adds the file.
	 *
	 * @param file
	 *            the file
	 */
	public void removeFile(final FfpFile file) {
		ffpFiles.remove(file);
	}

	/**
	 * Sets the status.
	 *
	 * @param status
	 *            the new status
	 */
	public void setStatus(final StatusEnum status) {
		this.status = status;
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public StatusEnum getStatus() {
		return this.status;
	}

	/**
	 * Sets the path.
	 *
	 * @param torrentPathUri
	 *            the new uri
	 */
	public void setTorrentPathUri(final URI torrentPathUri) {
		this.torrentPathUri = torrentPathUri;
		if (torrentPathUri != null) {
			this.torrentPath = Paths.get(torrentPathUri);
		} else {
			this.torrentPath = null;
		}
	}

	/**
	 * Gets the path uri.
	 *
	 * @return the path uri
	 */
	public URI getTorrentPathUri() {
		return this.torrentPathUri;
	}

	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	@Transient
	public Path getTorrentPath() {
		return this.torrentPath;
	}

	/**
	 * Sets the path.
	 *
	 * @param path
	 *            the new bt path
	 * @return the path
	 */
	@Transient
	public void setTorrentPath(final Path path) {
		if (path != null) {
			setTorrentPathUri(path.toUri());
		} else {
			setTorrentPathUri(null);
		}

	}

	/**
	 * Search an {@link FfpFile} by {@link URI}.
	 *
	 * @param uri
	 *            The searched {@link URI}
	 * @return the list
	 */
	public List<FfpFile> contains(final URI uri) {
		final List<FfpFile> result = getFfpFiles().parallelStream().filter(p -> Objects.equals(uri, p.getPathUri()))
				.collect(Collectors.toList());
		return result;
	}

	/**
	 * Search an {@link FfpFile} by {@link Path}.
	 *
	 * @param path
	 *            The searched {@link Path}
	 * @return the list
	 */
	public List<FfpFile> contains(final Path path) {
		return contains(path.toUri());
	}

	/**
	 * Return the last date when we compute the checksum adler32 for this file.
	 *
	 * @return the last date or null if never computed
	 */
	public LocalDateTime getAdler32Date() {
		final AtomicLong result = this.getFfpFiles().stream().parallel().collect(() -> new AtomicLong(0), (t, u) -> {
			long epoch = 0;
			if (!u.getAdler32Date().equals(LocalDateTime.MIN)) {
				epoch = u.getAdler32Date().toInstant(ZoneOffset.UTC).toEpochMilli();
			}
			if (t.get() < epoch) {
				t.set(epoch);
			}
		}, (t, u) -> {
			if (t.get() < u.get()) {
				t.set(u.get());
			}
		});

		// If the Adler32 was never computed for this item : result is equals 0
		LocalDateTime adler32Date = null;
		if (result.get() != 0) {
			adler32Date = LocalDateTime.ofInstant(Instant.ofEpochMilli(result.get()), ZoneOffset.UTC.normalized());
		}
		return adler32Date;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		boolean result = false;
		if (obj instanceof FfpItem) {
			final FfpItem i = (FfpItem) obj;
			result = Objects.equals(getFfpFiles(), i.getFfpFiles());
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(getFfpFiles());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String result;
		if (getFfpFiles().size() > 0) {
			result = getFfpFiles().get(0).toString();
		} else {
			result = super.toString();
		}
		return result;
	}

}
