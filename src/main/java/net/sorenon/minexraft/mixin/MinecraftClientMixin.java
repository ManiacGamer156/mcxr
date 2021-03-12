package net.sorenon.minexraft.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.MetricsData;
import net.minecraft.util.TickDurationMonitor;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.snooper.Snooper;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.sorenon.minexraft.XrCamera;
import net.sorenon.minexraft.accessor.FBAccessor;
import net.sorenon.minexraft.HelloOpenXR;
import net.sorenon.minexraft.MineXRaftClient;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.openxr.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.client.MinecraftClient.IS_SYSTEM_MAC;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> {

    @Shadow
    private Thread thread;

    @Shadow
    private volatile boolean running;

    @Shadow
    @Nullable
    private CrashReport crashReport;

    public MinecraftClientMixin(String string) {
        super(string);
    }

    @Shadow
    protected abstract boolean shouldMonitorTickDuration();

    @Shadow
    protected abstract void startMonitor(boolean active, @Nullable TickDurationMonitor monitor);

    @Shadow
    protected abstract void endMonitor(boolean active, @Nullable TickDurationMonitor monitor);

    @Shadow
    private Profiler profiler;

    @Shadow
    protected abstract void render(boolean tick);

    @Shadow
    public abstract void cleanUpAfterCrash();

    @Shadow
    public abstract void openScreen(@Nullable Screen screen);

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract CrashReport addDetailsToCrashReport(CrashReport report);

//    /**
//     * @author sorenon
//     */
//    @Overwrite
//    public void run() {
//        this.thread = Thread.currentThread();
//
//        try {
//            boolean bl = false;
//
//            while(this.running) {
//                if (this.crashReport != null) {
//                    printCrashReport(this.crashReport);
//                    return;
//                }
//
//                try {
//                    TickDurationMonitor tickDurationMonitor = TickDurationMonitor.create("Renderer");
//                    boolean bl2 = this.shouldMonitorTickDuration();
//                    this.startMonitor(bl2, tickDurationMonitor);
//                    this.profiler.startTick();
//                    this.render(!bl);
//                    this.profiler.endTick();
//                    this.endMonitor(bl2, tickDurationMonitor);
//                } catch (OutOfMemoryError var4) {
//                    if (bl) {
//                        throw var4;
//                    }
//
//                    this.cleanUpAfterCrash();
//                    this.openScreen(new OutOfMemoryScreen());
//                    System.gc();
//                    LOGGER.fatal((String)"Out of memory", (Throwable)var4);
//                    bl = true;
//                }
//            }
//        } catch (CrashException var5) {
//            this.addDetailsToCrashReport(var5.getReport());
//            this.cleanUpAfterCrash();
//            LOGGER.fatal((String)"Reported exception thrown!", (Throwable)var5);
//            printCrashReport(var5.getReport());
//        } catch (Throwable var6) {
//            CrashReport crashReport = this.addDetailsToCrashReport(new CrashReport("Unexpected error", var6));
//            LOGGER.fatal("Unreported exception thrown!", var6);
//            this.cleanUpAfterCrash();
//            printCrashReport(crashReport);
//        }
//
//    }

    @Shadow
    @Final
    private Window window;

    @Shadow
    public abstract void scheduleStop();

    @Shadow
    @Nullable
    private CompletableFuture<Void> resourceReloadFuture;

    @Shadow
    @Nullable
    public Overlay overlay;

    @Shadow
    public abstract CompletableFuture<Void> reloadResources();

    @Shadow
    @Final
    private Queue<Runnable> renderTaskQueue;

    @Shadow
    @Final
    private RenderTickCounter renderTickCounter;

    @Shadow
    public abstract void tick();

    @Shadow
    @Final
    public Mouse mouse;

    @Shadow
    @Final
    private SoundManager soundManager;

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Mutable
    @Shadow
    @Final
    private Framebuffer framebuffer;

    @Shadow
    public boolean skipGameRender;

    @Shadow
    private boolean paused;

    @Shadow
    private float pausedTickDelta;

    @Shadow
    @Final
    private ToastManager toastManager;

    @Shadow
    @Nullable
    private ProfileResult tickProfilerResult;

    @Shadow
    protected abstract void drawProfilerResults(MatrixStack matrices, ProfileResult profileResult);

    @Shadow
    protected abstract int getFramerateLimit();

    @Shadow
    private int fpsCounter;

    @Shadow
    public abstract boolean isIntegratedServerRunning();

    @Shadow
    @Nullable
    public Screen currentScreen;

    @Shadow
    @Final
    public MetricsData metricsData;

    @Shadow
    private long lastMetricsSampleTime;

    @Shadow
    private long nextDebugInfoUpdateTime;

    @Shadow
    private static int currentFps;

    @Shadow
    public String fpsDebugString;

    @Shadow
    @Final
    public GameOptions options;

    @Shadow
    @Final
    private Snooper snooper;

    @Shadow
    @Nullable
    private IntegratedServer server;

    @Shadow public abstract void method_29970(Screen screen);

    @Shadow public abstract void startIntegratedServer(String worldName);

    int colorTexture;
    Framebuffer leftEyeFramebuffer;
    XrCamera xrCamera;

    @Inject(method = "run", at = @At("HEAD"))
    void start(CallbackInfo ci) {
        HelloOpenXR helloOpenXR = MineXRaftClient.helloOpenXR;
        helloOpenXR.eventDataBuffer = XrEventDataBuffer.calloc();
        helloOpenXR.eventDataBuffer.type(XR10.XR_TYPE_EVENT_DATA_BUFFER);

        HelloOpenXR.Swapchain swapchain = helloOpenXR.swapchains[0];
        leftEyeFramebuffer = new Framebuffer(swapchain.width, swapchain.height, true, IS_SYSTEM_MAC);
        xrCamera = new XrCamera();
    }


    //    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"), method = "render")
//    void frameBufferDraw(Framebuffer framebuffer, int width, int height) {
//
//    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;render(Z)V"), method = "run")
    void loop(MinecraftClient minecraftClient, boolean tick) throws InterruptedException {
        HelloOpenXR helloOpenXR = MineXRaftClient.helloOpenXR;
        if (helloOpenXR.pollEvents()) {
            running = false;
            return;
        }

        if (helloOpenXR.sessionRunning) {
            helloOpenXR.renderFrameOpenXR((xrCompositionLayerProjectionView, xrSwapchainImageOpenGLKHR, integer) -> {
                if (integer == 1) {
//                    helloOpenXR.OpenGLRenderView(xrCompositionLayerProjectionView, xrSwapchainImageOpenGLKHR, integer);
                } else {
                    colorTexture = xrSwapchainImageOpenGLKHR.image();
                    ((FBAccessor) leftEyeFramebuffer).setColorTexture(xrSwapchainImageOpenGLKHR.image());
                    Framebuffer vanFramebuffer = framebuffer;
                    framebuffer = leftEyeFramebuffer;
                    MineXRaftClient.viewportRect = xrCompositionLayerProjectionView.subImage().imageRect();
                    MineXRaftClient.fov = xrCompositionLayerProjectionView.fov();
                    MineXRaftClient.pose = xrCompositionLayerProjectionView.pose();
                    renderXR(tick);
                    MineXRaftClient.pose = null;
                    MineXRaftClient.fov = null;
                    MineXRaftClient.viewportRect = null;
                    framebuffer = vanFramebuffer;
                }
                return null;
            });
        } else {
            // Throttle loop since xrWaitFrame won't be called.
            Thread.sleep(250);
        }

//        renderXR(tick);
    }

    //renderLayerOpenXR
    void renderXR(boolean tick) {
        this.window.setPhase("Pre render");
        long time = Util.getMeasuringTimeNano();
        if (this.window.shouldClose()) {
            this.scheduleStop();
        }

        if (this.resourceReloadFuture != null && !(this.overlay instanceof SplashScreen)) {
            CompletableFuture<Void> completableFuture = this.resourceReloadFuture;
            this.resourceReloadFuture = null;
            this.reloadResources().thenRun(() -> {
                completableFuture.complete(null);
            });
        }

        Runnable runnable;
        while ((runnable = this.renderTaskQueue.poll()) != null) {
            runnable.run();
        }

        int k;
        if (tick) {
            k = this.renderTickCounter.beginRenderTick(Util.getMeasuringTimeMs());
            this.profiler.push("scheduledExecutables");
            this.runTasks();
            this.profiler.pop();
            this.profiler.push("tick");

            for (int j = 0; j < Math.min(10, k); ++j) {
                this.profiler.visit("clientTick");
                this.tick();
            }

            this.profiler.pop();
        }

        this.mouse.updateMouse();
        this.window.setPhase("Render");
        this.profiler.push("sound");
        this.soundManager.updateListenerPosition(this.gameRenderer.getCamera());
        this.profiler.pop();
        //RENDER START
        //renderLayerOpenXR
        //foreach layer:
        //int fbColOrg = framebuffer.color
        //framebuffer.color = layer.color
        //viewport
        doRender(time, tick);
        //RENDER END
        //SCRAP START
//        RenderSystem.pushMatrix();
//        this.framebuffer.draw(this.window.getFramebufferWidth(), this.window.getFramebufferHeight());
//        RenderSystem.popMatrix();
//        this.profiler.swap("updateDisplay");
//        this.window.swapBuffers();
//        k = this.getFramerateLimit();
//        if ((double) k < Option.FRAMERATE_LIMIT.getMax()) {
//            RenderSystem.limitDisplayFPS(k);
//        }

        GLFW.glfwPollEvents();
        RenderSystem.replayQueue();
        Tessellator.getInstance().getBuffer().clear();

        //SCRAP END

        this.profiler.swap("yield");
//        Thread.yield();
        this.profiler.pop();
        this.window.setPhase("Post render");
        ++this.fpsCounter;
        boolean bl = this.isIntegratedServerRunning() && (this.currentScreen != null && this.currentScreen.isPauseScreen() || this.overlay != null && this.overlay.pausesGame()) && !this.server.isRemote();
        if (this.paused != bl) {
            if (this.paused) {
                this.pausedTickDelta = this.renderTickCounter.tickDelta;
            } else {
                this.renderTickCounter.tickDelta = this.pausedTickDelta;
            }

            this.paused = bl;
        }

        long m = Util.getMeasuringTimeNano();
        this.metricsData.pushSample(m - this.lastMetricsSampleTime);
        this.lastMetricsSampleTime = m;
        this.profiler.push("fpsUpdate");

        while (Util.getMeasuringTimeMs() >= this.nextDebugInfoUpdateTime + 1000L) {
            currentFps = this.fpsCounter;
            this.fpsDebugString = String.format("%d fps T: %s%s%s%s B: %d", currentFps, (double) this.options.maxFps == Option.FRAMERATE_LIMIT.getMax() ? "inf" : this.options.maxFps, this.options.enableVsync ? " vsync" : "", this.options.graphicsMode.toString(), this.options.cloudRenderMode == CloudRenderMode.OFF ? "" : (this.options.cloudRenderMode == CloudRenderMode.FAST ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius);
            this.nextDebugInfoUpdateTime += 1000L;
            this.fpsCounter = 0;
            this.snooper.update();
            if (!this.snooper.isActive()) {
                this.snooper.method_5482();
            }
        }

        this.profiler.pop();
    }

    private void doRender(long time, boolean tick) {
//        int colAttachOrg = ((FBAccessor)framebuffer).getColorTexture();
//        ((FBAccessor)framebuffer).setColorTexture(colorTexture);

        this.profiler.push("render");
        RenderSystem.pushMatrix();
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, IS_SYSTEM_MAC);
        this.framebuffer.beginWrite(true);
        BackgroundRenderer.method_23792();
        this.profiler.push("display");
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        this.profiler.pop();
        if (!this.skipGameRender) {
            this.profiler.swap("gameRenderer");
            this.gameRenderer.render(this.paused ? this.pausedTickDelta : this.renderTickCounter.tickDelta, time, tick);
            this.profiler.swap("toasts");
            this.toastManager.draw(new MatrixStack());
            this.profiler.pop();
        }

        if (this.tickProfilerResult != null) {
            this.profiler.push("fpsPie");
            this.drawProfilerResults(new MatrixStack(), this.tickProfilerResult);
            this.profiler.pop();
        }

        this.profiler.push("blit");
        this.framebuffer.endWrite();
        RenderSystem.popMatrix();

//        ((FBAccessor)framebuffer).setColorTexture(colAttachOrg);
    }
}
