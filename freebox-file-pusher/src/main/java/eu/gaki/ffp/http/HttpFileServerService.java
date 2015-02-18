package eu.gaki.ffp.http;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

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

	/** The http file server. */
	private HttpFileServer httpFileServer;

	/**
	 * Instantiates a new http file server service.
	 *
	 * @param httpFileServer
	 *            the http file server
	 */
	public HttpFileServerService(HttpFileServer httpFileServer) {
		this.httpFileServer = httpFileServer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handle(final Request request, final Response response) {
		// request.getValue("If-Range");
		final String range = request.getValue("Range");

		final String target = request.getPath().getName();

		final Path pathToServe = httpFileServer.getFileToServe(target);
		if (pathToServe != null && Files.exists(pathToServe)) {
			LOGGER.info("Send resource {}", pathToServe);
			if (range != null && range.trim().length() != 0) {
				sendRange(response, pathToServe, range);
			} else {
				sendAll(request, response, pathToServe);
			}
		} else {
			LOGGER.info("Couldn't found resource {}", target);
			sendError(response,Status.NOT_FOUND);
		}

	}
	
	/**
	 * Send error.
	 *
	 * @param response the response
	 * @param status the status
	 */
	private void sendError(Response response, Status status) {
		response.setStatus(status);
		try {
			response.close();
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	

	/**
	 * Detect transfert end.
	 *
	 * @param response
	 *            the response
	 * @param path
	 *            the file
	 */
	private void detectTransfertEnd(final Response response, Path path) {
		
		try {
			if (response.getResponseTime() > 0) {
				// Get response observer
				Field observerField = response.getClass().getDeclaredField("observer");
				observerField.setAccessible(true);
				Object responseObserver = (Object) observerField.get(response);
				// Get error flag value
				Field errorField = responseObserver.getClass().getDeclaredField("error");
				errorField.setAccessible(true);
				AtomicBoolean error = (AtomicBoolean) errorField.get(responseObserver);
				if (!error.get()) {
					LOGGER.trace("Transfert end for {}", path);
					httpFileServer.removeAndDeleteFileToServe(path);
				}
			}
		} catch (NoSuchFieldException  | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			LOGGER.error(e.getMessage(), e);
		} 
	}

	/**
	 * Send all.
	 *
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @param file
	 *            the file
	 */
	private void sendAll(Request request, final Response response, final Path file) {
		sendRange(response, file, "bytes=0-");
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
	private void sendRange(final Response response, final Path file, final String range) {

		final String[] typeValue = range.split("=");
		if (typeValue.length == 2 && "bytes".equals(typeValue[0].trim())) {
			final String[] rangeIntervals = typeValue[1].trim().split(",[ ]*");
			if (rangeIntervals.length == 1) {
				final String[] rangeInterval = rangeIntervals[0].trim().split("-");

				try (WritableByteChannel output = response.getByteChannel(); FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {

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
					response.setValue("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"");
					long contentLength = lastBytePosition - firstBytePosition + 1;
					response.setContentLength(contentLength);
					response.setValue("Content-Range", "bytes " + firstBytePosition + "-" + lastBytePosition + "/" + lengthBytes);
					// response.setValue("Last-Modified", "");
					channel.transferTo(firstBytePosition, contentLength, output);

					if (lastBytePosition == lengthBytes - 1) {
						// It's the last range
						detectTransfertEnd(response, file);
					}

				} catch (final IOException e) {
					LOGGER.error(e.getMessage(), e);
					sendError(response,Status.INTERNAL_SERVER_ERROR);
				}

			} else {
				LOGGER.error("Don't support multiple range.");
				sendError(response,Status.REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		} else {
			LOGGER.error("Only support bytes range.");
			sendError(response,Status.REQUESTED_RANGE_NOT_SATISFIABLE);
		}
	}

}
