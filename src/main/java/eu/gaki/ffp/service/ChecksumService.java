/*
 *
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Adler32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;

// TODO: Auto-generated Javadoc
/**
 * CheckedInputStream.
 */
public class ChecksumService {

	/** The Constant CHUNK_SIZE. */
	private static final int CHUNK_SIZE = 52428800; // 1024 * 1024 * 10 = 10MB

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ChecksumService.class);

	/** The checksum use size. */
	private final Boolean checksumUseSize;

	/**
	 * Instantiates a new checksum service.
	 *
	 * @param configService
	 *            the config service
	 */
	public ChecksumService(final ConfigService configService) {
		checksumUseSize = configService.isChecksumUseSize();
	}

	/**
	 * Compute the checksun of the FfpItem.
	 *
	 * @param item
	 *            The FfpItem.
	 * @return true if the checksum have changed
	 */
	public boolean computeChecksum(final FfpItem item) {
		final Instant start = Instant.now();
		LOGGER.trace("Start compute the checksum of {}.", item);
		final AtomicBoolean result = item.getFfpFiles().stream().parallel().collect(() -> new AtomicBoolean(false),
				(t, u) -> {
					t.set(t.get() || computeChecksum(u, CHUNK_SIZE));
				}, (t, u) -> {
					t.set(t.get() || u.get());
				});
		final boolean b = result.get();
		final Instant end = Instant.now();
		LOGGER.trace("Stop compute the checksum of {}. Result have change: {}. Took {}",
				new Object[] { item, b, Duration.between(start, end) });
		return b;
	}

	/**
	 * Compute checksum.
	 *
	 * @param file
	 *            the file
	 * @param chunkSize
	 *            the chunk size
	 * @return true, if successful
	 */
	protected boolean computeChecksum(final FfpFile file, final int chunkSize) {
		boolean result;
		if (checksumUseSize) {
			result = computeChecksumSize(file);
		} else {
			result = computeChecksumAdler32(file, chunkSize);
		}
		return result;
	}

	/**
	 * Compute checksum by using only the file size (for low CPU).
	 *
	 * @param file
	 *            the file
	 * @return true, if successful
	 */
	protected boolean computeChecksumSize(final FfpFile file) {
		boolean result = false;
		final Map<Long, Long> newAdler32 = new HashMap<>();
		if (file != null && file.getPath() != null) {
			final Path path = file.getPath();
			if (Files.isRegularFile(path)) {
				try {
					final long newChecksum = Files.size(path);
					final Long oldChecksum = file.getAdler32().get(0L);
					if (oldChecksum == null || !oldChecksum.equals(newChecksum)) {
						result = true;
					}
					newAdler32.put(0L, newChecksum);
					if (result) {
						file.setAdler32Date(LocalDateTime.now());
						file.setAdler32(newAdler32);
						LOGGER.info("Checksum changed {}.", path);
					}
				} catch (final IOException e) {
					LOGGER.error("Checksum compute faild for {" + path + "}", e);
				}
			}
		}

		return result;
	}

	/**
	 * Compute the checksum of the file by using Adler32.
	 *
	 * @param file
	 *            The file.
	 * @param chunkSize
	 *            The size of each chunk
	 * @return true if the checksum have changed
	 */
	protected boolean computeChecksumAdler32(final FfpFile file, final int chunkSize) {
		boolean result = false;
		final Map<Long, Long> newAdler32 = new HashMap<>();
		if (file != null && file.getPath() != null) {
			final Path path = file.getPath();
			if (Files.isRegularFile(path)) {
				try (FileChannel inChannel = FileChannel.open(path, StandardOpenOption.READ)) {
					// Compute buffer size
					int bufferSize;
					if (inChannel.size() < chunkSize) {
						bufferSize = (int) inChannel.size();
					} else {
						bufferSize = chunkSize;
					}

					// Create a buffer
					final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

					// inChannel.close();
					int noOfBytesRead = inChannel.read(buffer);
					final Adler32 adler32 = new Adler32();
					while (noOfBytesRead != -1) {
						buffer.flip();
						final byte[] readed = new byte[noOfBytesRead];
						buffer.get(readed);
						adler32.update(readed);
						final Long newChecksum = adler32.getValue();
						adler32.reset();
						final Long position = Long.valueOf(inChannel.position());
						final Long oldChecksum = file.getAdler32().get(position);
						if (oldChecksum == null || !oldChecksum.equals(newChecksum)) {
							result = true;
						}
						newAdler32.put(position, newChecksum);
						buffer.clear();
						noOfBytesRead = inChannel.read(buffer);
					}
					buffer.clear();
					if (result) {
						file.setAdler32Date(LocalDateTime.now());
						file.setAdler32(newAdler32);
						LOGGER.info("Checksum changed {}.", path);
					}
				} catch (final IOException e) {
					LOGGER.error("Checksum compute faild for {" + path + "}", e);
				}
			}
		}
		return result;
	}

}
