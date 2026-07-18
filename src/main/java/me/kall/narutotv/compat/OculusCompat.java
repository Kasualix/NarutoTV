package me.kall.narutotv.compat;

import net.irisshaders.iris.api.v0.IrisApi;

public class OculusCompat implements ICompat {
    @Override
    public boolean shaderUsing() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
