package org.CoreBytes.opdash.client.BlockOverlay;

import com.mojang.blaze3d.systems.RenderSystem;
import org.CoreBytes.opdash.client.Config.ConfigManager;
import org.lwjgl.opengl.GL11;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class DarkOakHighlighter {

    public static final int DEFAULT_RADIUS = 32;
    public static final int MAX_RADIUS = 60;
    public static final float DEFAULT_RED = 0.60f;
    public static final float DEFAULT_GREEN = 0.00f;
    public static final float DEFAULT_BLUE = 0.60f;
    public static final float DEFAULT_ALPHA = 0.08f;
    private static final float FACE_OFFSET = 0.002f;
    private static float red = DEFAULT_RED;
    private static float green = DEFAULT_GREEN;
    private static float blue = DEFAULT_BLUE;
    private static float alpha = DEFAULT_ALPHA;
    private static int radius = DEFAULT_RADIUS;
    private static final Set<Block> HIGHLIGHT_BLOCKS = new HashSet<>();
    private static final List<BlockPos> CACHED_MATCHES = new ArrayList<>();
    private static final List<BlockPos> BUILDING_MATCHES = new ArrayList<>();
    private static final int SCAN_BUDGET_PER_FRAME = 22000;
    private static final int MAX_RENDERED_BLOCKS = 12000;
    private static final int NEAR_SCAN_RADIUS = 8;
    private static final int NEAR_RENDER_BUDGET = 2000;
    private static final int RESCAN_MOVE_THRESHOLD = 4;
    private static BlockPos scanCenter = BlockPos.ORIGIN;
    private static int scanX;
    private static int scanY;
    private static int scanZ;
    private static boolean scanComplete = true;
    private static int settingsRevision = 0;
    private static int appliedRevision = -1;
    private static int lastPublishedSize = 0;

    public static void loadFromConfig(ConfigManager config) {
        if (config == null) {
            return;
        }

        red = clamp01(config.getOverlayRed());
        green = clamp01(config.getOverlayGreen());
        blue = clamp01(config.getOverlayBlue());
        alpha = clamp01(config.getOverlayAlpha());
        radius = clampRadius(config.getOverlayRadius());

        HIGHLIGHT_BLOCKS.clear();
        List<String> blocks = config.getOverlayBlocks();
        for (String id : blocks) {
            Identifier identifier = parseBlockIdentifier(id);
            if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
                continue;
            }

            Block block = Registries.BLOCK.get(identifier);
            if (block == null) {
                continue;
            }

            HIGHLIGHT_BLOCKS.add(block);
        }
        settingsRevision++;
    }

    public static void renderOverlay(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        World world = client.world;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        if (shouldResetScan(playerPos)) {
            resetScan(playerPos);
            appliedRevision = settingsRevision;
        }

        continueScan(world);

        if (HIGHLIGHT_BLOCKS.isEmpty()) {
            return;
        }
        int radiusSquared = radius * radius;
        int nearRadius = Math.min(NEAR_SCAN_RADIUS, radius);
        int nearRadiusSquared = nearRadius * nearRadius;
        int nearRenderLimit = Math.min(NEAR_RENDER_BUDGET, MAX_RENDERED_BLOCKS);
        int farRenderLimit = MAX_RENDERED_BLOCKS - nearRenderLimit;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = null;
        int renderedNear = 0;
        int renderedFar = 0;

        // Always scan near the player each frame so close blocks appear immediately.
        BlockPos.Mutable nearPos = new BlockPos.Mutable();
        for (int x = -nearRadius; x <= nearRadius; x++) {
            int xSquared = x * x;
            for (int y = -nearRadius; y <= nearRadius; y++) {
                int xySquared = xSquared + (y * y);
                if (xySquared > nearRadiusSquared) {
                    continue;
                }
                for (int z = -nearRadius; z <= nearRadius; z++) {
                    int distSq = xySquared + (z * z);
                    if (distSq > nearRadiusSquared) {
                        continue;
                    }

                    nearPos.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    Block block = world.getBlockState(nearPos).getBlock();
                    if (!HIGHLIGHT_BLOCKS.contains(block)) {
                        continue;
                    }

                    if (buffer == null) {
                        buffer = beginOverlayBuffer(tessellator);
                    }
                    renderSurfaceOverlay(buffer, world, nearPos, camPos, red, green, blue, alpha);
                    renderedNear++;
                    if (renderedNear >= nearRenderLimit) {
                        break;
                    }
                }
                if (renderedNear >= nearRenderLimit) {
                    break;
                }
            }
            if (renderedNear >= nearRenderLimit) {
                break;
            }
        }

        // Render far blocks; while scan is running, use current building list so radius keeps filling.
        List<BlockPos> farSource = BUILDING_MATCHES.isEmpty() ? CACHED_MATCHES : BUILDING_MATCHES;
        if (farRenderLimit > 0 && !farSource.isEmpty()) {
            for (int i = 0; i < farSource.size(); i++) {
                BlockPos pos = farSource.get(i);
                int dx = pos.getX() - playerPos.getX();
                int dy = pos.getY() - playerPos.getY();
                int dz = pos.getZ() - playerPos.getZ();
                int distSq = (dx * dx) + (dy * dy) + (dz * dz);
                if (distSq > radiusSquared || distSq <= nearRadiusSquared) {
                    continue;
                }
                // Cached scan entries can become stale (e.g. block mined); revalidate against world state.
                if (!HIGHLIGHT_BLOCKS.contains(world.getBlockState(pos).getBlock())) {
                    continue;
                }

                if (buffer == null) {
                    buffer = beginOverlayBuffer(tessellator);
                }
                renderSurfaceOverlay(buffer, world, pos, camPos, red, green, blue, alpha);
                renderedFar++;
                if (renderedFar >= farRenderLimit) {
                    break;
                }
            }
        }

        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void renderSurfaceOverlay(BufferBuilder buffer, World world, BlockPos pos,
                                             Vec3d camPos,
                                             float r, float g, float b, float alpha) {

        float minX = (float) (pos.getX() - camPos.x);
        float minY = (float) (pos.getY() - camPos.y);
        float minZ = (float) (pos.getZ() - camPos.z);
        float maxX = minX + 1.0f;
        float maxY = minY + 1.0f;
        float maxZ = minZ + 1.0f;

        faceXY(buffer, minX, maxX, minY, minZ, maxZ, maxY + FACE_OFFSET, r, g, b, alpha);
        faceXY(buffer, minX, maxX, minY, minZ, maxZ, minY - FACE_OFFSET, r, g, b, alpha);
        faceXZ(buffer, minX, maxX, minY, maxY, minZ - FACE_OFFSET, r, g, b, alpha);
        faceXZ(buffer, minX, maxX, minY, maxY, maxZ + FACE_OFFSET, r, g, b, alpha);
        faceYZ(buffer, minY, maxY, minZ, maxZ, minX - FACE_OFFSET, r, g, b, alpha);
        faceYZ(buffer, minY, maxY, minZ, maxZ, maxX + FACE_OFFSET, r, g, b, alpha);
    }

    private static void faceXY(BufferBuilder buffer, float minX, float maxX, float yMin, float minZ, float maxZ,
                               float y, float r, float g, float b, float a) {
        buffer.vertex(minX, y, minZ).color(r, g, b, a);
        buffer.vertex(maxX, y, minZ).color(r, g, b, a);
        buffer.vertex(maxX, y, maxZ).color(r, g, b, a);
        buffer.vertex(minX, y, maxZ).color(r, g, b, a);
    }

    private static void faceXZ(BufferBuilder buffer, float minX, float maxX, float minY, float maxY,
                               float z, float r, float g, float b, float a) {
        buffer.vertex(minX, minY, z).color(r, g, b, a);
        buffer.vertex(maxX, minY, z).color(r, g, b, a);
        buffer.vertex(maxX, maxY, z).color(r, g, b, a);
        buffer.vertex(minX, maxY, z).color(r, g, b, a);
    }

    private static void faceYZ(BufferBuilder buffer, float minY, float maxY, float minZ, float maxZ,
                               float x, float r, float g, float b, float a) {
        buffer.vertex(x, minY, minZ).color(r, g, b, a);
        buffer.vertex(x, minY, maxZ).color(r, g, b, a);
        buffer.vertex(x, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(x, maxY, minZ).color(r, g, b, a);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int clampRadius(int value) {
        return Math.max(1, Math.min(MAX_RADIUS, value));
    }

    private static BufferBuilder beginOverlayBuffer(Tessellator tessellator) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        return tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    private static Identifier parseBlockIdentifier(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }

        Identifier id = Identifier.tryParse(trimmed);
        if (id != null) {
            return id;
        }

        if (!trimmed.contains(":")) {
            return Identifier.tryParse("minecraft:" + trimmed);
        }
        return null;
    }

    private static void resetScan(BlockPos center) {
        scanCenter = center.toImmutable();
        BUILDING_MATCHES.clear();
        lastPublishedSize = 0;
        scanX = -radius;
        scanY = -radius;
        scanZ = -radius;
        scanComplete = false;
    }

    private static void continueScan(World world) {
        if (scanComplete || HIGHLIGHT_BLOCKS.isEmpty()) {
            return;
        }

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        int baseX = scanCenter.getX();
        int baseY = scanCenter.getY();
        int baseZ = scanCenter.getZ();
        int scanRadiusSquared = radius * radius;
        int budget = SCAN_BUDGET_PER_FRAME;

        while (!scanComplete && budget-- > 0) {
            int xySquared = (scanX * scanX) + (scanY * scanY);
            if (xySquared + (scanZ * scanZ) <= scanRadiusSquared) {
                mutablePos.set(baseX + scanX, baseY + scanY, baseZ + scanZ);
                Block block = world.getBlockState(mutablePos).getBlock();
                if (HIGHLIGHT_BLOCKS.contains(block)) {
                    BUILDING_MATCHES.add(mutablePos.toImmutable());
                }
            }

            scanZ++;
            if (scanZ > radius) {
                scanZ = -radius;
                scanY++;
                if (scanY > radius) {
                    scanY = -radius;
                    scanX++;
                    if (scanX > radius) {
                        scanComplete = true;
                    }
                }
            }
        }

        // Publish partial progress so far radius appears before the full scan is done.
        if (scanComplete
                || BUILDING_MATCHES.size() - lastPublishedSize >= 128
                || (lastPublishedSize == 0 && !BUILDING_MATCHES.isEmpty())) {
            CACHED_MATCHES.clear();
            CACHED_MATCHES.addAll(BUILDING_MATCHES);
            lastPublishedSize = BUILDING_MATCHES.size();
        }
    }

    private static boolean shouldResetScan(BlockPos playerPos) {
        if (appliedRevision != settingsRevision) {
            return true;
        }

        int dx = playerPos.getX() - scanCenter.getX();
        int dy = playerPos.getY() - scanCenter.getY();
        int dz = playerPos.getZ() - scanCenter.getZ();
        int distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);

        // While scanning, still recenter if player moved enough that radius feels "stuck".
        if (!scanComplete) {
            int hardThreshold = Math.max(RESCAN_MOVE_THRESHOLD, NEAR_SCAN_RADIUS + 4);
            return distanceSquared > (hardThreshold * hardThreshold);
        }

        int threshold = Math.max(RESCAN_MOVE_THRESHOLD, radius / 2);
        return distanceSquared > (threshold * threshold);
    }

}
