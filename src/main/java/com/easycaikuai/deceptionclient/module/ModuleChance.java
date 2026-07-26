package com.easycaikuai.deceptionclient.module;

import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import java.util.Random;

/**
 * 概率触发工具 —— 给模块添加 Chance 属性，控制功能触发概率。
 * 用法: if (ModuleChance.shouldTrigger(this)) { ... }
 */
public class ModuleChance {
    private static final Random RNG = new Random();

    /** 获取 Chance 属性，不存在则返回 100（总是触发） */
    public static PercentProperty getChance(Module module) {
        return (PercentProperty) com.easycaikuai.deceptionclient.Deception.propertyManager
                .getProperty(module, "Chance");
    }

    /** 判断本次是否应该触发（根据 Chance 百分比） */
    public static boolean shouldTrigger(Module module) {
        PercentProperty chance = getChance(module);
        if (chance == null) return module.isEnabled();
        return RNG.nextDouble() <= chance.getValue() / 100.0;
    }
}
