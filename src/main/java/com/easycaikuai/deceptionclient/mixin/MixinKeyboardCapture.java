package com.easycaikuai.deceptionclient.mixin;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.easycaikuai.deceptionclient.event.EventManager;
import com.easycaikuai.deceptionclient.events.KeyEvent;

/**
 * 捕捉未注册按键（如 RSHIFT）的按下状态。
 * Minecraft 的 runTick 只对已注册 KeyBinding 调用 setKeyBindState，
 * 导致 RSHIFT 等修饰键不会触发 KeyEvent。
 * 此 Mixin 在 runTick 尾部直接检查键盘状态来补充。
 */
@Mixin(Minecraft.class)
public class MixinKeyboardCapture {
    private static final int[] TRACKED_KEYS = {
        Keyboard.KEY_RSHIFT, Keyboard.KEY_LSHIFT,
        Keyboard.KEY_RCONTROL, Keyboard.KEY_LCONTROL,
        Keyboard.KEY_RMENU, Keyboard.KEY_LMENU
    };

    private static final boolean[] prevStates = new boolean[TRACKED_KEYS.length];
    private static long lastGuiClose = 0L;

    @Inject(method = "runTick", at = @At("TAIL"))
    private void onRunTickTail(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null && mc.theWorld != null) {
            // GUI 关闭后 300ms 内不触发，防止 RSHIFT 弹起/按下造成死循环
            if (System.currentTimeMillis() - lastGuiClose < 300) return;

            for (int i = 0; i < TRACKED_KEYS.length; i++) {
                int keyCode = TRACKED_KEYS[i];
                boolean isDown = Keyboard.isKeyDown(keyCode);
                if (isDown && !prevStates[i]) {
                    EventManager.call(new KeyEvent(keyCode));
                }
                prevStates[i] = isDown;
            }
        } else if (mc.currentScreen != null) {
            lastGuiClose = System.currentTimeMillis();
        }
    }
}
