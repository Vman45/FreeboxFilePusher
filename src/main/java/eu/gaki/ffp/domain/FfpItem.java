/*
 *
 */
package eu.gaki.ffp.domain;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
	 * Search an {@link FfpFile} by {@link URI}
	 *
	 * @param uri
	 *            The searched {@link URI}
	 * @return
	 */
	public List<FfpFile> contains(final URI uri) {
		final List<FfpFile> result = getFfpFiles().parallelStream().filter(p -> Objects.equals(uri, p.getPathUri()))
				.collect(Collectors.toList());
		return result;
	}

	/**
	 * Search an {@link FfpFile} by {@link Path}
	 *
	 * @param path
	 *            The searched {@link Path}
	 * @return
	 */
	public List<FfpFile> contains(final Path path) {
		return contains(path.toUri());
	}

	/**
	 * Return the last date when we compute the checksome adler32 for this file.
	 *
	 * @return the last date
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

		return LocalDateTime.ofInstant(Instant.ofEpochMilli(result.get()), ZoneOffset.UTC.normalized());
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

}
