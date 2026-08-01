package com.easycaikuai.deceptionclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.easycaikuai.deceptionclient.command.CommandManager;
import com.easycaikuai.deceptionclient.command.commands.*;
import com.easycaikuai.deceptionclient.config.Config;
import com.easycaikuai.deceptionclient.event.EventManager;
import com.easycaikuai.deceptionclient.font.FontManager;
import com.easycaikuai.deceptionclient.management.*;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.module.ModuleManager;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.PropertyManager;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

public class Deception {
    public static String clientName = "&l[&b&lDeception&f&l]&r ";
    public static String version;
    public static RotationManager rotationManager;
    public static FloatManager floatManager;
    public static BlinkManager blinkManager;
    public static DelayManager delayManager;
    public static LagManager lagManager;
    public static PlayerStateManager playerStateManager;
    public static FriendManager friendManager;
    public static TargetManager targetManager;
    public static PropertyManager propertyManager;
    public static ModuleManager moduleManager;
    public static CommandManager commandManager;
    public static FontManager fontManager;

    public Deception() {
        this.init();
    }

    public void init() {
        rotationManager = new RotationManager();
        floatManager = new FloatManager();
        blinkManager = new BlinkManager();
        delayManager = new DelayManager();
        lagManager = new LagManager();
        playerStateManager = new PlayerStateManager();
        friendManager = new FriendManager();
        targetManager = new TargetManager();
        propertyManager = new PropertyManager();
        moduleManager = new ModuleManager();
        ensureAllModulesRegistered();
        commandManager = new CommandManager();
        fontManager = new FontManager();
        fontManager.load();
        EventManager.register(rotationManager);
        EventManager.register(floatManager);
        EventManager.register(blinkManager);
        EventManager.register(delayManager);
        EventManager.register(lagManager);
        EventManager.register(moduleManager);
        EventManager.register(commandManager);
        commandManager.commands.add(new BindCommand());
        commandManager.commands.add(new ConfigCommand());
        commandManager.commands.add(new DenickCommand());
        commandManager.commands.add(new FriendCommand());
        commandManager.commands.add(new HelpCommand());
        commandManager.commands.add(new HideCommand());
        commandManager.commands.add(new IgnCommand());
        commandManager.commands.add(new ItemCommand());
        commandManager.commands.add(new ListCommand());
        commandManager.commands.add(new ModuleCommand());
        commandManager.commands.add(new PlayerCommand());
        commandManager.commands.add(new ShowCommand());
        commandManager.commands.add(new TargetCommand());
        commandManager.commands.add(new ToggleCommand());
        commandManager.commands.add(new VclipCommand());
        for (Module module : moduleManager.modules.values()) {
            ArrayList<Property<?>> properties = new ArrayList<>();
            for (final Field field : module.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                final Object obj;
                try {
                    obj = field.get(module);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                if (obj instanceof Property<?>) {
                    ((Property<?>) obj).setOwner(module);
                    properties.add((Property<?>) obj);
                }
            }
            propertyManager.properties.put(module.getClass(), properties);
            EventManager.register(module);
        }
        Config config = new Config("default", true);
        if (config.file.exists()) {
            config.load();
        }
        if (friendManager.file.exists()) {
            friendManager.load();
        }
        if (targetManager.file.exists()) {
            targetManager.load();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(config::save));

        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Deception.class.getResourceAsStream("/version.json")), StandardCharsets.UTF_8)) {
            JsonObject modInfo = new JsonParser().parse(reader).getAsJsonObject();
            version = modInfo.get("version").getAsString();
        } catch (Exception e) {
            version = "dev";
        }
    }

    /** 手动注册所有模块（补充自动扫描遗漏的） */
    private void ensureAllModulesRegistered() {

        try {
            register(new com.easycaikuai.deceptionclient.module.modules.combat.AimAssist());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.AntiFireball());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.AutoBlock());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.AutoClicker());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.AutoLava());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.BackTrack());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.BlockHit());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.HitBox());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.JumpReset());
            try { register(new com.easycaikuai.deceptionclient.module.modules.combat.KillAura()); } catch (Exception e) {}
            register(new com.easycaikuai.deceptionclient.module.modules.combat.LagRange());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.MoreKB());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.Reach());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.TargetStrafe());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.Timer());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.TickBase());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.Velocity());
            register(new com.easycaikuai.deceptionclient.module.modules.combat.Wtap());

            register(new com.easycaikuai.deceptionclient.module.modules.movement.AntiAFK());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.AntiVoid());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.Blink());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.Eagle());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.Fly());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.Jesus());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.KeepSprint());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.LongJump());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.NoFall());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.NoJumpDelay());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.NoSlow());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.SafeWalk());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.Speed());
            register(new com.easycaikuai.deceptionclient.module.modules.movement.Sprint());

            register(new com.easycaikuai.deceptionclient.module.modules.render.Animations());
            register(new com.easycaikuai.deceptionclient.module.modules.render.BedESP());
            register(new com.easycaikuai.deceptionclient.module.modules.render.BlockOverlay());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Chams());
            register(new com.easycaikuai.deceptionclient.module.modules.render.ChestESP());
            register(new com.easycaikuai.deceptionclient.module.modules.render.ChineseHat());
            register(new com.easycaikuai.deceptionclient.module.modules.render.DynamicIsland());
            register(new com.easycaikuai.deceptionclient.module.modules.render.ESP());
            register(new com.easycaikuai.deceptionclient.module.modules.render.FreeLook());
            register(new com.easycaikuai.deceptionclient.module.modules.render.FullBright());
            register(new com.easycaikuai.deceptionclient.module.modules.render.GuiModule());
            register(new com.easycaikuai.deceptionclient.module.modules.render.HUD());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Indicators());
            register(new com.easycaikuai.deceptionclient.module.modules.render.ItemESP());
            register(new com.easycaikuai.deceptionclient.module.modules.render.KillEffect());
            register(new com.easycaikuai.deceptionclient.module.modules.render.NameTags());
            register(new com.easycaikuai.deceptionclient.module.modules.render.NoHurtCam());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Notification());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Potion());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Radar());
            register(new com.easycaikuai.deceptionclient.module.modules.render.TargetESP());
            register(new com.easycaikuai.deceptionclient.module.modules.render.TargetHUD());
            register(new com.easycaikuai.deceptionclient.module.modules.render.TeamHealthDisplay());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Tracers());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Trajectories());
            register(new com.easycaikuai.deceptionclient.module.modules.render.ViewClip());
            register(new com.easycaikuai.deceptionclient.module.modules.render.WaterMark());
            register(new com.easycaikuai.deceptionclient.module.modules.render.Xray());
            register(new com.easycaikuai.deceptionclient.module.modules.render.BetterFps());
            register(new com.easycaikuai.deceptionclient.module.modules.render.MotionBlur());
            register(new com.easycaikuai.deceptionclient.module.modules.render.BindGUI());

            register(new com.easycaikuai.deceptionclient.module.modules.player.AntiDebuff());
            register(new com.easycaikuai.deceptionclient.module.modules.player.AutoBlockIn());
            register(new com.easycaikuai.deceptionclient.module.modules.player.AutoHeal());
            register(new com.easycaikuai.deceptionclient.module.modules.player.AutoSwap());
            register(new com.easycaikuai.deceptionclient.module.modules.player.AntiBot());
            register(new com.easycaikuai.deceptionclient.module.modules.player.AutoTool());
            register(new com.easycaikuai.deceptionclient.module.modules.player.ChestAura());
            register(new com.easycaikuai.deceptionclient.module.modules.player.ChestStealer());
            register(new com.easycaikuai.deceptionclient.module.modules.player.FastBow());
            register(new com.easycaikuai.deceptionclient.module.modules.player.FastPlace());
            register(new com.easycaikuai.deceptionclient.module.modules.player.GhostHand());
            register(new com.easycaikuai.deceptionclient.module.modules.player.InvManager());
            register(new com.easycaikuai.deceptionclient.module.modules.player.Refill());
            register(new com.easycaikuai.deceptionclient.module.modules.player.Scaffold());
            register(new com.easycaikuai.deceptionclient.module.modules.player.Telly());
            register(new com.easycaikuai.deceptionclient.module.modules.player.SpeedMine());
            register(new com.easycaikuai.deceptionclient.module.modules.player.Stuck());

            register(new com.easycaikuai.deceptionclient.module.modules.misc.AntiObbyTrap());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.AntiObfuscate());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.AutoL());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.BedNuker());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.BedTracker());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.Disabler());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.Displace());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.FakeLag());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.FlagDetector());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.LightningTracker());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.NickHider());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.NoRotate());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.ServerLag());
            register(new com.easycaikuai.deceptionclient.module.modules.misc.Spammer());

            System.out.println("[Deception] Manually registered " + moduleManager.modules.size() + " modules");
        } catch (Exception e) {
            System.err.println("[Deception] Module registration error: " + e.getMessage());
        }
    }

    private void register(com.easycaikuai.deceptionclient.module.Module module) {
        if (moduleManager.modules.containsKey(module.getClass())) return;
        moduleManager.modules.put(module.getClass(), module);

        // 注册属性
        java.util.ArrayList<com.easycaikuai.deceptionclient.property.Property<?>> props = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : module.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object obj = field.get(module);
                if (obj instanceof com.easycaikuai.deceptionclient.property.Property) {
                    ((com.easycaikuai.deceptionclient.property.Property<?>) obj).setOwner(module);
                    props.add((com.easycaikuai.deceptionclient.property.Property<?>) obj);
                }
            } catch (IllegalAccessException e) {}
        }
        propertyManager.properties.put(module.getClass(), props);

        // 注册事件
        com.easycaikuai.deceptionclient.event.EventManager.register(module);
    }
}
