package com.easycaikuai.deceptionclient.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventManager;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.module.modules.combat.*;
import com.easycaikuai.deceptionclient.module.modules.movement.*;
import com.easycaikuai.deceptionclient.module.modules.player.*;
import com.easycaikuai.deceptionclient.module.modules.render.*;
import com.easycaikuai.deceptionclient.module.modules.misc.*;
import com.easycaikuai.deceptionclient.property.Property;
import java.lang.reflect.Field;
import java.util.ArrayList;

@Mixin(Minecraft.class)
public class MixinHUDRegistry {

    private static boolean checked = false;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTick(CallbackInfo ci) {
        if (checked) return;
        if (Deception.moduleManager == null || Deception.propertyManager == null) return;
        checked = true;

        // 注册属性（已有模块）
        for (Module module : Deception.moduleManager.modules.values()) {
            if (Deception.propertyManager.properties.containsKey(module.getClass())
                    && !Deception.propertyManager.properties.get(module.getClass()).isEmpty()) continue;
            registerProperties(module);
        }
    }

    private void registerProperties(Module module) {
        ArrayList<Property<?>> props = new ArrayList<>();
        for (Field field : module.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object obj = field.get(module);
                if (obj instanceof Property) {
                    ((Property<?>) obj).setOwner(module);
                    props.add((Property<?>) obj);
                }
            } catch (IllegalAccessException e) {
                System.err.println("[Deception] Failed to access property for " + module.getName() + ": " + e.getMessage());
            }
        }
        Deception.propertyManager.properties.put(module.getClass(), props);
        EventManager.register(module);
    }
}
