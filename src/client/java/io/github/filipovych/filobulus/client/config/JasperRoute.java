package io.github.filipovych.filobulus.client.config;

public enum JasperRoute {
	DEFAULT("jasper", "Default (47 stops)"),
	FAILSAYFE("jasper_failsayfe", "Failsayfe (39 stops)");

	private final String routeId;
	private final String label;

	JasperRoute(String routeId, String label) {
		this.routeId = routeId;
		this.label = label;
	}

	public String routeId() {
		return routeId;
	}

	@Override
	public String toString() {
		return label;
	}
}
