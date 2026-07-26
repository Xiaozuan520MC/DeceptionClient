package com.easycaikuai.deceptionclient.mixin;

import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.module.modules.render.Chams;
import com.easycaikuai.deceptionclient.module.modules.render.ViewClip;
import com.easycaikuai.deceptionclient.module.modules.render.Xray;

@SideOnly(Side.CLIENT)
@Mixin({VisGraph.class})
public abstract class MixinVisGraph {
    @Inject(
            method = {"func_178606_a"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void func_178606_a(CallbackInfo callbackInfo) {
        if (Deception.moduleManager != null) {
            if (Deception.moduleManager.modules.get(Chams.class).isEnabled()
                    || Deception.moduleManager.modules.get(ViewClip.class).isEnabled()
                    || Deception.moduleManager.modules.get(Xray.class).isEnabled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(
            method = {"computeVisibility"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void computeVisibility(CallbackInfoReturnable<SetVisibility> callbackInfoReturnable) {
        if (Deception.moduleManager != null) {
            if (Deception.moduleManager.modules.get(Chams.class).isEnabled()
                    || Deception.moduleManager.modules.get(ViewClip.class).isEnabled()
                    || Deception.moduleManager.modules.get(Xray.class).isEnabled()) {
                SetVisibility setVisibility = new SetVisibility();
                setVisibility.setAllVisible(true);
                callbackInfoReturnable.setReturnValue(setVisibility);
            }
        }
    }
}
