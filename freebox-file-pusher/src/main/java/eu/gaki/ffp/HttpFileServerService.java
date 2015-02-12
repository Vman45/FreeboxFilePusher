/**
 *
 */
package eu.gaki.ffp;

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

	// request.getValue("If-Range");
	final String range = request.getValue("Range");

	final Path path = request.getPath();
	final String target = path.getPath();

	final File file = new File("D:\\" + target);
	if (range != null && range.trim().length() != 0) {
	    sendRange(response, file, range);
	} else {
	    sendAll(response, file);
	}

	// Try to detect when the file upload is complete
	if (false) {
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
			    e.printStackTrace();
			}
		    }

		    System.err.println("Finish: " + response.getResponseTime());

		}
	    }).start();
	}

    }

    /**
     * Send all.
     *
     * @param response
     *            the response
     * @param file
     *            the file
     */
    private void sendAll(final Response response, final File file) {
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
	    channel.transferTo(0, lengthBytes, output);
	} catch (final IOException e) {
	    response.setStatus(Status.INTERNAL_SERVER_ERROR);
	    e.printStackTrace();
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
		final String[] rangeInterval = typeValue[0].trim().split("-");

		final long firstBytePosition = Long.valueOf(rangeInterval[0].trim());
		final long bytesToSend = Long.valueOf(rangeInterval[1].trim()) - firstBytePosition + 1;
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
			WritableByteChannel output = response.getByteChannel();
			FileChannel channel = randomAccessFile.getChannel()) {
		    // Total length
		    final long lengthBytes = channel.size();

		    long lastBytePosition = firstBytePosition + bytesToSend - 1;
		    if (lastBytePosition >= lengthBytes) {
			lastBytePosition = lengthBytes - 1;
		    }

		    response.setStatus(Status.PARTIAL_CONTENT);
		    response.setDate("Date", System.currentTimeMillis());
		    response.setValue("Accept-Ranges", "bytes");
		    response.setContentType("application/octetstream");
		    response.setContentLength(lastBytePosition - firstBytePosition + 1);
		    response.setValue("Content-Range", "bytes " + firstBytePosition + "-" + lastBytePosition + "/"
			    + lengthBytes);
		    // response.setValue("Last-Modified", "");
		    channel.transferTo(0, lastBytePosition - firstBytePosition + 1, output);
		} catch (final IOException e) {
		    response.setStatus(Status.INTERNAL_SERVER_ERROR);
		    e.printStackTrace();
		}

	    } else {
		response.setStatus(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
	    }
	} else {
	    response.setStatus(Status.REQUESTED_RANGE_NOT_SATISFIABLE);
	}
    }

}
