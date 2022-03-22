package com.cleanroommc.modularui.common.widget;

import com.cleanroommc.modularui.api.Interactable;
import com.cleanroommc.modularui.api.math.Size;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public class ButtonWidget extends SyncedWidget implements Interactable {

    private BiConsumer<ClickData, Widget> clickAction;

    public ButtonWidget setOnClick(BiConsumer<ClickData, Widget> clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    @Override
    protected @NotNull Size determineSize(int maxWidth, int maxHeight) {
        return new Size(20, 20);
    }

    @Override
    public boolean onClick(int buttonId, boolean doubleClick) {
        if (clickAction != null) {
            ClickData clickData = ClickData.create(buttonId, doubleClick);
            clickAction.accept(clickData, this);
            if (syncsToServer()) {
                syncToServer(1, clickData::writeToPacket);
            }
            Interactable.playButtonClickSound();
            return true;
        }
        return false;
    }

    @Override
    public void readOnClient(int id, PacketBuffer buf) {
    }

    @Override
    public void readOnServer(int id, PacketBuffer buf) {
        if (id == 1) {
            ClickData data = ClickData.readPacket(buf);
            clickAction.accept(data, this);
        }
    }
}
