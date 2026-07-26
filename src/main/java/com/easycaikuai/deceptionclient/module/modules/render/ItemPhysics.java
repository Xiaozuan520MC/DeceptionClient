package com.easycaikuai.deceptionclient.module.modules.render;


import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;

public class ItemPhysics extends Module {
    public ItemPhysics(){super("ItemPhysics",false,false);}
    public static IntProperty rollSpeed = new IntProperty("Roll Speed",10,1,20);
}
