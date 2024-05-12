package com.haberno.habernoprinter.mixin;

import com.google.common.collect.ImmutableList;
import com.haberno.habernoprinter.LitematicaMixinMod;
import fi.dy.masa.litematica.gui.GuiConfigs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = GuiConfigs.class, remap = false)
public class GuiConfigsMixin {

    @Redirect(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<IConfigBase> moreOptions() {
        return LitematicaMixinMod.getConfigList();
    }

    @Redirect(method = "getConfigs", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private List<ConfigHotkey> moreHotkeys() {
        return LitematicaMixinMod.getHotkeyList();
    }
}