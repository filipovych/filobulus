package io.github.filipovych.filobulus.client.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

// one download from hypixel's public api, the lists that need no key
// never call it on the game thread, it waits for the answer
public final class PublicApi {
	private static final Duration TIMEOUT = Duration.ofSeconds(8);
	// for the whole answer, the timeout of the request stops counting once the headers are in
	private static final long ANSWER_SECONDS = 20;

	public interface Parser<T> {
		T parse(Reader text) throws IOException;
	}

	private PublicApi() {
	}

	public static <T> T get(URI uri, Parser<T> parser) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(TIMEOUT)
				.header("User-Agent", "Filobulus")
				.header("Accept-Encoding", "gzip")
				.build();
		try (HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
			HttpResponse<byte[]> response;
			try {
				response = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).get(ANSWER_SECONDS, TimeUnit.SECONDS);
			} catch (TimeoutException | ExecutionException e) {
				// or closing the client would wait for the dead request
				http.shutdownNow();
				throw new IOException(e instanceof TimeoutException ? "no answer in time" : String.valueOf(e.getCause()));
			} catch (InterruptedException e) {
				http.shutdownNow();
				throw e;
			}
			if (response.statusCode() != 200) {
				throw new IOException("status " + response.statusCode());
			}
			boolean gzip = response.headers().firstValue("Content-Encoding").orElse("").equalsIgnoreCase("gzip");
			InputStream body = new ByteArrayInputStream(response.body());
			try (Reader text = new InputStreamReader(gzip ? new GZIPInputStream(body) : body, StandardCharsets.UTF_8)) {
				return parser.parse(text);
			}
		}
	}
}
