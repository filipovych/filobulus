package io.github.filipovych.filobulus.client.render;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.Guard;
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
	// argb, glacite blue for the next stop and white for the ones after it, each fainter than the one before
	private static final int NEXT = 0xFF7FD4FF;
	private static final int NEXT_FILL = 0x507FD4FF;
	private static final int LATER = 0xFFFFFF;
	private static final int LATER_ALPHA = 0xB0;
	private static final int LATER_FADE = 0x26;
	private static final int FAINTEST = 0x30;
	// amber for the block the next stop aims at
	private static final int AIM = 0xFFFFB347;
	private static final int AIM_FILL = 0x40FFB347;
	private static final float STROKE = 2.5f;
	// labels keep their size on screen past this many blocks
	private static final double LABEL_FULL_SIZE_RANGE = 8;

	private RouteRenderer() {
	}

	// called right before the game collects its gizmos for the frame, so ours are drawn with them
	public static void draw(LevelRenderContext context) {
		List<Waypoint> stops = FilobulusClient.tracker().upcoming();
		if (stops.isEmpty() || !RoutesConfig.showRoutes) {
			return;
		}
		Guard.run("route drawing", () -> {
			CameraRenderState camera = context.levelState().cameraRenderState;
			Waypoint next = stops.get(0);
			drawStop(next, camera.pos, GizmoStyle.strokeAndFill(NEXT, STROKE, NEXT_FILL), NEXT);
			next.aim().ifPresent(aim -> drawAim(next, aim));
			for (int i = 1; i < stops.size() && i < RoutesConfig.stopsShown; i++) {
				int later = Math.max(FAINTEST, LATER_ALPHA - (i - 1) * LATER_FADE) << 24 | LATER;
				drawStop(stops.get(i), camera.pos, GizmoStyle.stroke(later, STROKE), later);
			}
			// one block in front of the camera is where the crosshair points
			Vec3 crosshair = camera.pos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot));
			Gizmos.line(crosshair, centre(next), NEXT, STROKE).setAlwaysOnTop();
		});
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

	// the block to point at from the stop, with a line from the stop to it
	private static void drawAim(Waypoint stop, Waypoint.Aim aim) {
		Gizmos.cuboid(new AABB(aim.x(), aim.y(), aim.z(), aim.x() + 1, aim.y() + 1, aim.z() + 1),
				GizmoStyle.strokeAndFill(AIM, STROKE, AIM_FILL)).setAlwaysOnTop();
		Gizmos.line(centre(stop), new Vec3(aim.x() + 0.5, aim.y() + 0.5, aim.z() + 0.5), AIM, STROKE).setAlwaysOnTop();
	}

	private static Vec3 centre(Waypoint stop) {
		return new Vec3(stop.x() + 0.5, stop.y() + 0.5, stop.z() + 0.5);
	}
}
