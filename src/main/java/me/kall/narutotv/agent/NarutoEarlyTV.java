package me.kall.narutotv.agent;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.renderer.BufferFrameRenderer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class NarutoEarlyTV extends AbstractTV<ByteBuffer> {
    private static final NarutoEarlyTV INSTANCE = new NarutoEarlyTV();

    public NarutoEarlyTV() {
        super(new BufferFrameRenderer.Early());
    }

    @SuppressWarnings("unused")
    public static void bridge() {
        INSTANCE.render();
    }

    @Override
    public boolean isRunnable() {
        if (RenderProps.isEnd()) {
            this.shutdownEntire(true);
            return false;
        }
        return true;
    }

    @Override
    public void render() {
        super.render();
        if (this.video != null) {
            LifetimeController life = this.video.life();
            if (life != null) {
                RenderProps.markCost(life.sinceSetupSec());
            }
        }
    }

    @Override
    protected @NotNull MediaArgs newArgs() {
        MediaArgs mediaArgs = Sources.random(true);
        RenderProps.saveInit(mediaArgs);
        return mediaArgs;
    }

    @Override
    protected float initVolume() {
        return 1.0F;
    }
}
