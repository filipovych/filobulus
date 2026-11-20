package io.github.filipovych.filobulus.client.render;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.RoutesConfig;
import io.github.filipovych.filobulus.route.Waypoint;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// draws the route with minecraft's own debug shapes (gizmos), everything version fragile about drawing is in here
public final class RouteRenderer {
	// argb, glacite blue for the next stop and a faded white for the one after it
	private static final int NEXT = 0xFF7FD4FF;
	private static final int NEXT_FILL = 0x507FD4FF;
	private static final int LATER = 0xB0FFFFFF;
	private static final float STROKE = 2.5f;
	// labels keep their size on screen past this many blocks
	private static final double LABEL_FULL_SIZE_RANGE = 8;

	private static boolean failed;

	private RouteRenderer() {
	}

	// called right before the game collects its gizmos for the frame, so ours are drawn with them
	public static void draw(LevelRenderContext context) {
		List<Waypoint> stops = FilobulusClient.tracker().upcoming();
		if (stops.isEmpty() || !RoutesConfig.showRoutes) {
			return;
		}
		try {
			CameraRenderState camera = context.levelState().cameraRenderState;
			Waypoint next = stops.get(0);
			drawStop(next, camera.pos, GizmoStyle.strokeAndFill(NEXT, STROKE, NEXT_FILL), NEXT);
			if (stops.size() > 1) {
				drawStop(stops.get(1), camera.pos, GizmoStyle.stroke(LATER, STROKE), LATER);
			}
			// one block in front of the camera is where the crosshair points
			Vec3 crosshair = camera.pos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot));
			Gizmos.line(crosshair, centre(next), NEXT, STROKE).setAlwaysOnTop();
		} catch (RuntimeException e) {
			// a drawing problem must never take the game down, say it once and carry on
			if (!failed) {
				failed = true;
				FilobulusClient.LOGGER.error("Could not draw the route", e);
			}
		}
	}

	private static void drawStop(Waypoint stop, Vec3 camera, GizmoStyle style, int labelColor) {
		Gizmos.cuboid(new AABB(stop.x(), stop.y(), stop.z(), stop.x() + 1, stop.y() + 1, stop.z() + 1), style)
				.setAlwaysOnTop();
		Vec3 above = centre(stop).add(0, 1, 0);
		float scale = TextGizmo.Style.DEFAULT_SCALE
				* (float) Math.max(1, camera.distanceTo(above) / LABEL_FULL_SIZE_RANGE);
		Gizmos.billboardText(stop.label(), above, TextGizmo.Style.forColorAndCentered(labelColor).withScale(scale))
				.setAlwaysOnTop();
	}

	private static Vec3 centre(Waypoint stop) {
		return new Vec3(stop.x() + 0.5, stop.y() + 0.5, stop.z() + 0.5);
	}
}
