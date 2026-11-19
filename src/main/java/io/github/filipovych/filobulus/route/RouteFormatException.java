package io.github.filipovych.filobulus.route;

public class RouteFormatException extends Exception {
	public RouteFormatException(String message) {
		super(message);
	}

	public RouteFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
