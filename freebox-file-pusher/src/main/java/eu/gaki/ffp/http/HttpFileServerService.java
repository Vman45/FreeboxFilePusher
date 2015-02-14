/**
 *
 */
package eu.gaki.ffp.http;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import org.simpleframework.http.Path;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HttpFileServerService.
 *
 * @author frup59604
 */
public class HttpFileServerService implements Container {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpFileServerService.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handle(final Request request, final Response response) {
		// http://192.168.1.42/Usenet/Mad Max Au-delà Du Dôme Du Tonnerre
		// (1985)/Mad.Max.Beyond.Thunderdome.1985.MULTi.1080p.BluRay.x264-ROUGH/mad.max.beyond.thunderdome.1985.multi.1080p.bluray.x264-rough.mkv
		// http://192.168.1.42/toto.mkv
		// request.getValue("If-Range");
		final String range = request.getValue("Range");

		final Path path = request.getPath();
		final String target = path.getPath();

		final File file = new File("F:\\" + target);
		if (range != null && range.trim().length() != 0) {
			sendRange(response, file, range);
		} else {
			sendAll(request, response, file);
		}

	}

	private void detectTransfertEnd(final Response response) {
		// Try to detect when the file upload is complete
		new Thread(new Runnable() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void run() {

				while (response.getResponseTime() <= 0) {
					try {
						Thread.sleep(1000);
					} catch (final InterruptedException e) {
						LOGGER.error(e.getMessage(),e);
					}
				}

				System.err.println("Finish: " + response.getResponseTime());

			}
		}).start();
	}

	/**
	 * Send all.
	 * 
	 * @param request
	 *
	 * @param response
	 *            the response
	 * @param file
	 *            the file
	 */
	private void sendAll(Request request, final Response response, final File file) {
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
				WritableByteChannel output = response.getByteChannel();
				FileChannel channel = randomAccessFile.getChannel()) {
			// Total length
			final long lengthBytes = channel.size();
			response.setStatus(Status.OK);
			response.setDate("Date", System.currentTimeMillis());
			response.setContentType("application/octetstream");
			response.setValue("Accept-Ranges", "bytes");
			// response.setValue("Last-Modified", "");
			response.setContentLength(lengthBytes);
			if (!"HEAD".equals(request.getMethod())) {
				channel.transferTo(0, lengthBytes, output);
				detectTransfertEnd(response);
			}
		} catch (final IOException e) {
			response.setStatus(Status.INTERNAL_SERVER_ERROR);
			LOGGER.error(e.getMessage(),e);
		}
	}

	/**
	 * Send range.
	 *
	 * @param response
	 *            the response
	 * @param file
	 *            the file
	 * @param range
	 *            the range
	 */
	private void sendRange(final Response response, final File file, final String range) {

		final String[] typeValue = range.split("=");
		if (typeValue.length == 2 && "bytes".equals(typeValue[0].trim())) {
			final String[] rangeIntervals = typeValue[1].trim().split(",[ ]*");
			if (rangeIntervals.length == 1) {
				final String[] rangeInterval = rangeIntervals[0].trim().split("-");

				try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
						WritableByteChannel output = response.getByteChannel();
						FileChannel channel = randomAccessFile.getChannel()) {
					// Total length
					final long lengthBytes = channel.size();

					// Compute the byte to start
					final long firstBytePosition = Long.valueOf(rangeInterval[0].trim());

					// Compute the byte to end
					long lastBytePosition;
					if (rangeInterval.length == 2) {
						// A end is specified: use it
						lastBytePosition = Long.valueOf(rangeInterval[1].trim());
					} else {
						// no endding specified: send all remaining
						lastBytePosition = lengthBytes - 1;
					}

					response.setStatus(Status.PARTIAL_CONTENT);
					response.setDate("Date", System.currentTimeMillis());
					response.setValue("Accept-Ranges", "bytes");
					response.setContentType("application/octetstream");
					long contentLength = lastBytePosition - firstBytePosition + 1;
					response.setContentLength(contentLength);
					response.setValue("Content-Range", "bytes " + firstBytePosition + "-" + lastBytePosition + "/" + lengthBytes);
					// response.setValue("Last-Modified", "");
					channel.transferTo(firstBytePosition, contentLength, output);

					if (lastBytePosition == lengthBytes - 1) {
						// It's the last part
						detectTransfertEnd(response);
					}

				} catch (final IOException e) {
					response.setStatus(Status.INTERNAL_SERVER_ERROR);
					LOGGER.error(e.getMessage(),e);
				}

			} else {
				response.setStatus(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		} else {
			response.setStatus(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
		}
	}

}
