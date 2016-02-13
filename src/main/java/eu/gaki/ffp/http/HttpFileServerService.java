package eu.gaki.ffp.http;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
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
	private final HttpFileServer httpFileServer;

	/** The configuration. */
	private final Properties configuration;

	/**
	 * Instantiates a new http file server service.
	 *
	 * @param httpFileServer
	 *            the http file server
	 */
	public HttpFileServerService(final Properties configuration, final HttpFileServer httpFileServer) {
		this.configuration = configuration;
		this.httpFileServer = httpFileServer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handle(final Request request, final Response response) {
		// request.getValue("If-Range");

		final String requestName = request.getPath().getName();

		final String rssFileString = configuration.getProperty("rss.location", "rss.xml");
		final Path rssPath = FileSystems.getDefault().getPath(rssFileString);

		Path pathToServe = null;
		if (requestName != null && requestName.equals(rssPath.getFileName().toString())) {
			pathToServe = rssPath;
		} else {
			pathToServe = httpFileServer.getFileToServe(requestName);
		}
		sendFile(request, response, pathToServe, pathToServe.equals(rssPath));
	}

	/**
	 * Send the file by HTTP.
	 *
	 * @param request
	 *            the HTTP request
	 * @param response
	 *            the HTTP response
	 * @param pathToServe
	 *            the file path to serve
	 * @param forceReadOnly
	 *            the force read only (deletion forbiden)
	 */
	private void sendFile(final Request request, final Response response, final Path pathToServe, final boolean forceReadOnly) {
		// See if a range request has been done
		final String range = request.getValue("Range");
		if (pathToServe != null && Files.exists(pathToServe)) {
			LOGGER.info("Send resource {}", pathToServe);
			if (range != null && range.trim().length() != 0) {
				sendRange(response, pathToServe, range, forceReadOnly);
			} else {
				sendAll(request, response, pathToServe, forceReadOnly);
			}
		} else {
			LOGGER.error("Couldn't found resource {}", pathToServe);
			sendError(response, Status.NOT_FOUND);
		}
	}

	/**
	 * Send error.
	 *
	 * @param response
	 *            the response
	 * @param status
	 *            the status
	 */
	private void sendError(final Response response, final Status status) {
		response.setStatus(status);
		try {
			response.close();
		} catch (final IOException e) {
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
	 * @param forceReadOnly
	 *            the force read only (deletion forbiden)
	 */
	private void detectTransfertEnd(final Response response, final Path path, final boolean forceReadOnly) {

		try {
			if (response.getResponseTime() > 0) {
				// Get response observer
				final Field observerField = response.getClass().getDeclaredField("observer");
				observerField.setAccessible(true);
				final Object responseObserver = observerField.get(response);
				// Get error flag value
				final Field errorField = responseObserver.getClass().getDeclaredField("error");
				errorField.setAccessible(true);
				final AtomicBoolean error = (AtomicBoolean) errorField.get(responseObserver);
				if (!error.get()) {
					LOGGER.trace("Transfert end for {}", path);
					if (!forceReadOnly) {
						httpFileServer.removeAndDeleteFileToServe(path);
					}
				}
			}
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
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
	 * @param forceReadOnly
	 *            the force read only (deletion forbiden)
	 */
	private void sendAll(final Request request, final Response response, final Path file, final boolean forceReadOnly) {
		sendRange(response, file, "bytes=0-", forceReadOnly);
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
	 * @param forceReadOnly
	 *            the force read only (deletion forbiden)
	 */
	private void sendRange(final Response response, final Path file, final String range, final boolean forceReadOnly) {

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
					final long contentLength = lastBytePosition - firstBytePosition + 1;
					response.setContentLength(contentLength);
					response.setValue("Content-Range", "bytes " + firstBytePosition + "-" + lastBytePosition + "/" + lengthBytes);
					// response.setValue("Last-Modified", "");
					channel.transferTo(firstBytePosition, contentLength, output);

					if (lastBytePosition == lengthBytes - 1) {
						// It's the last range
						detectTransfertEnd(response, file, forceReadOnly);
					}

				} catch (final IOException e) {
					LOGGER.error(e.getMessage(), e);
					sendError(response, Status.INTERNAL_SERVER_ERROR);
				}

			} else {
				LOGGER.error("Don't support multiple range.");
				sendError(response, Status.REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		} else {
			LOGGER.error("Only support bytes range.");
			sendError(response, Status.REQUESTED_RANGE_NOT_SATISFIABLE);
		}
	}

}
