/*
 * 
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Adler32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;

/**
 * CheckedInputStream.
 */
public class ChecksumService {

	/** The Constant CHUNK_SIZE. */
	private static final int CHUNK_SIZE = 104857600; // 1024* 1024 * 100 = 100MB

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ChecksumService.class);

	/**
	 * Compute the checksun of the FfpItem.
	 *
	 * @param item
	 *            The FfpItem.
	 * @return true if the checksum have changed
	 */
	public boolean computeChecksum(final FfpItem item) {
		AtomicBoolean result = item
				.getFfpFiles()
				.stream()
				.parallel()
				.collect(() -> new AtomicBoolean(false),
						(t, u) -> t.set(t.get() || computeChecksum(u)),
						(t, u) -> t.set(t.get() || u.get()));
		return result.get();
	}

	/**
	 * Compute the checksun of the file.
	 *
	 * @param file
	 *            The file.
	 * @return true if the checksum have changed
	 */
	public boolean computeChecksum(final FfpFile file) {
		return this.computeChecksum(file, CHUNK_SIZE);
	}

	/**
	 * Compute the checksun of the file.
	 *
	 * @param file
	 *            The file.
	 * @param chunkSize
	 *            The size of each chunk
	 * @return true if the checksum have changed
	 */
	public boolean computeChecksum(final FfpFile file, final int chunkSize) {
		boolean result = false;
		final Map<Long, Long> newAdler32 = new HashMap<>();
		if (file != null && file.getPath() != null) {
			final Path path = file.getPath();
			if (Files.isRegularFile(path)) {
				try (FileChannel inChannel = FileChannel.open(path,
						StandardOpenOption.READ)) {
					// Compute buffer size
					int bufferSize;
					if (inChannel.size() < Integer.MAX_VALUE) {
						bufferSize = (int) inChannel.size();
					} else {
						bufferSize = Integer.MAX_VALUE;
					}
					
					// Create a buffer
//					final MappedByteBuffer buffer = inChannel.map(
//							FileChannel.MapMode.READ_ONLY, 0, bufferSize);
					final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
					
					inChannel.close();
					
					final Adler32 adler32 = new Adler32();
					while (buffer.remaining() > 0) {
						byte[] readed;
						if (buffer.remaining() >= chunkSize) {
							readed = new byte[chunkSize];
						} else {
							readed = new byte[buffer.remaining()];
						}
						buffer.get(readed);
						adler32.update(readed);
						final Long newChecksum = adler32.getValue();
						adler32.reset();
						final Long position = Long.valueOf(buffer.position());
						final Long oldChecksum = file.getAdler32()
								.get(position);
						if (oldChecksum == null
								|| !oldChecksum.equals(newChecksum)) {
							result = true;
						}
						newAdler32.put(position, newChecksum);
					}
					buffer.clear();
					file.setAdler32Date(LocalDateTime.now());
					file.setAdler32(newAdler32);
					if (result) {
						LOGGER.info("Checksum changed {" + path + "}.");
					}
				} catch (final IOException e) {
					LOGGER.error("Checksum compute faild for {" + path + "}", e);
				}
			}
		}
		return result;
	}

}
