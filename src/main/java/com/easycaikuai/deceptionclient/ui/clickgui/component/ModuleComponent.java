package com.easycaikuai.deceptionclient.ui.clickgui.component;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.*;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUIGlobal;
import com.easycaikuai.deceptionclient.ui.clickgui.component.value.*;
import com.easycaikuai.deceptionclient.util.animation.Easing;
import com.easycaikuai.deceptionclient.util.animation.RiseAnim;
import com.easycaikuai.deceptionclient.util.gui.GUIUtil;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ModuleComponent {
    public Module module;
    public double x, y, width, height;
    public List<ValueComponent> values = new ArrayList<>();
    private RiseAnim hoverAnim = new RiseAnim(Easing.EASE_OUT_EXPO, 200L);
    private boolean binding = false;

    public ModuleComponent(Module module) {
        this.module = module;
        this.height = 38.0F;
        if (Deception.propertyManager.properties.containsKey(module.getClass())) {
            for (Property<?> prop : Deception.propertyManager.properties.get(module.getClass())) {
                if (prop instanceof BooleanProperty) values.add(new BooleanValueComponent(prop));
                else if (prop instanceof ModeProperty) values.add(new ModeValueComponent(prop));
                else if (prop instanceof ColorProperty) values.add(new ColorValueComponent(prop));
                else if (prop instanceof FloatProperty || prop instanceof IntProperty || prop instanceof PercentProperty)
                    values.add(new NumberValueComponent(prop));
            }
        }
    }

    public void render(RiseClickGUI gui, int mouseX, int mouseY, float partialTicks) {
        boolean hover = GUIUtil.mouseOver(this.x, this.y, this.width, 22.0D, mouseX, mouseY);
        hoverAnim.run(hover ? 1.0D : 0.0D);

        // Module background
        Color bg = ColorUtil.withAlpha(gui.moduleBgColor, (int) (gui.moduleBgColor.getAlpha() * (0.5 + 0.5 * hoverAnim.getValue())));
        RoundedUtils.drawRound((float) this.x, (float) this.y, (float) this.width, (float) this.height, 4.0F, bg);

        // Module name
        int nameColor = module.isEnabled() ? gui.getAccentColor().getRGB() : gui.fontDarkColor.getRGB();
        Fonts.interBold.get(15.0F).drawString(module.getName(), (float) this.x + 6.0F, (float) this.y + 4.0F, nameColor);

        // Binding indicator
        if (binding) {
            Fonts.interBold.get(13.0F).drawString("...", (float) this.x + 6.0F, (float) this.y + 20.0F, gui.fontDarkerColor.getRGB());
        } else {
            // Keybind
            String keyText = module.getKey() != 0 ? "Key: " + com.easycaikuai.deceptionclient.util.KeyBindUtil.getKeyName(module.getKey()) : "";
            if (!keyText.isEmpty()) {
                Fonts.interBold.get(13.0F).drawString(keyText, (float) this.x + 6.0F, (float) this.y + 20.0F, gui.fontDarkerColor.getRGB());
            }
        }

        // Draw values
        double valY = this.y + 38.0F;
        for (ValueComponent vc : values) {
            vc.draw(this.x + 8.0F, valY, mouseX, mouseY, partialTicks);
            valY += vc.getHeight();
        }

        // Update height
        this.height = 38.0F + values.stream().mapToDouble(ValueComponent::getHeight).sum();
    }

    public void click(int mouseX, int mouseY, int button) {
        if (GUIUtil.mouseOver(this.x, this.y, this.width, 22.0D, mouseX, mouseY)) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                binding = !binding;
            }
            return;
        }
        // Forward to values
        for (ValueComponent vc : values) {
            vc.click(mouseX, mouseY, button);
        }
    }

    public void released() {
        for (ValueComponent vc : values) vc.released();
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == 1) { // ESC
                binding = false;
                return;
            }
            module.setKey(keyCode);
            binding = false;
        }
    }
}