package me.kall.narutotv.compat;

import net.irisshaders.iris.api.v0.IrisApi;

public class IrisCompat implements ICompat {
    @Override
    public boolean shaderUsing() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
