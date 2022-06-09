package com.cleanroommc.modularui.common.internal.wrapper;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.drawable.GuiHelper;
import com.cleanroommc.modularui.api.drawable.Text;
import com.cleanroommc.modularui.api.math.Alignment;
import com.cleanroommc.modularui.api.math.Color;
import com.cleanroommc.modularui.api.math.Pos2d;
import com.cleanroommc.modularui.api.math.Size;
import com.cleanroommc.modularui.api.screen.Cursor;
import com.cleanroommc.modularui.api.screen.ModularUIContext;
import com.cleanroommc.modularui.api.screen.ModularWindow;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.api.widget.IWidgetParent;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.api.widget.Widget;
import com.cleanroommc.modularui.common.internal.mixin.GuiContainerMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class ModularGui extends GuiContainer {

    public static boolean debugMode = ModularUIConfig.debug;

    private final ModularUIContext context;
    private Pos2d mousePos = Pos2d.ZERO;

    @Nullable
    private Interactable lastClicked;
    private long lastClick = -1;
    private long lastFocusedClick = -1;
    private int drawCalls = 0;
    private long drawTime = 0;
    private int fps = 0;

    public ModularGui(ModularUIContainer container) {
        super(container);
        this.context = container.getContext();
        this.context.initializeClient(this);
    }

    public ModularUIContext getContext() {
        return context;
    }

    public Cursor getCursor() {
        return context.getCursor();
    }

    public Pos2d getMousePos() {
        return mousePos;
    }

    @Override
    public void onResize(@NotNull Minecraft mc, int w, int h) {
        super.onResize(mc, w, h);
        context.resize(new Size(w, h));
    }

    public void setMainWindowArea(Pos2d pos, Size size) {
        this.guiLeft = pos.x;
        this.guiTop = pos.y;
        this.xSize = size.width;
        this.ySize = size.height;
    }

    @Override
    public void initGui() {
        super.initGui();
        context.resize(new Size(width, height));
        this.context.buildWindowOnStart();
        this.context.getCurrentWindow().onOpen();
    }

    public GuiContainerMixin getAccessor() {
        return (GuiContainerMixin) this;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mousePos = new Pos2d(mouseX, mouseY);

        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        // mainly for invtweaks compat
        drawVanillaElements(mouseX, mouseY, partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        getAccessor().setHoveredSlot(null);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();
        this.drawGuiContainerForegroundLayer(partialTicks, mouseX, mouseY);
        RenderHelper.enableGUIStandardItemLighting();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(i, j, 0);
        MinecraftForge.EVENT_BUS.post(new GuiContainerEvent.DrawForeground(this, mouseX, mouseY));
        GlStateManager.popMatrix();

        InventoryPlayer inventoryplayer = this.mc.player.inventory;
        ItemStack itemstack = getAccessor().getDraggedStack().isEmpty() ? inventoryplayer.getItemStack() : getAccessor().getDraggedStack();
        GlStateManager.translate((float) i, (float) j, 0.0F);
        if (!itemstack.isEmpty()) {
            int k2 = getAccessor().getDraggedStack().isEmpty() ? 8 : 16;
            String s = null;

            if (!getAccessor().getDraggedStack().isEmpty() && getAccessor().getIsRightMouseClick()) {
                itemstack = itemstack.copy();
                itemstack.setCount(MathHelper.ceil((float) itemstack.getCount() / 2.0F));
            } else if (this.dragSplitting && this.dragSplittingSlots.size() > 1) {
                itemstack = itemstack.copy();
                itemstack.setCount(getAccessor().getDragSplittingRemnant());

                if (itemstack.isEmpty()) {
                    s = "" + TextFormatting.YELLOW + "0";
                }
            }

            this.drawItemStack(itemstack, mouseX - i - 8, mouseY - j - k2, s);
        }

        if (!getAccessor().getReturningStack().isEmpty()) {
            float f = (float) (Minecraft.getSystemTime() - getAccessor().getReturningStackTime()) / 100.0F;

            if (f >= 1.0F) {
                f = 1.0F;
                getAccessor().setReturningStack(ItemStack.EMPTY);
            }

            int l2 = getAccessor().getReturningStackDestSlot().xPos - getAccessor().getTouchUpX();
            int i3 = getAccessor().getReturningStackDestSlot().yPos - getAccessor().getTouchUpY();
            int l1 = getAccessor().getTouchUpX() + (int) ((float) l2 * f);
            int i2 = getAccessor().getTouchUpY() + (int) ((float) i3 * f);
            this.drawItemStack(getAccessor().getReturningStack(), l1, i2, null);
        }

        GlStateManager.popMatrix();

        if (debugMode) {
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            drawDebugScreen();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
    }

    private void drawItemStack(ItemStack stack, int x, int y, String altText) {
        GlStateManager.translate(0.0F, 0.0F, 32.0F);
        this.zLevel = 200.0F;
        this.itemRender.zLevel = 200.0F;
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        if (font == null) font = fontRenderer;
        this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlayIntoGUI(font, stack, x, y - (getAccessor().getDraggedStack().isEmpty() ? 0 : 8), altText);
        this.zLevel = 0.0F;
        this.itemRender.zLevel = 0.0F;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (debugMode) {
            long time = Minecraft.getSystemTime() / 1000;
            if (drawTime != time) {
                fps = drawCalls;
                drawCalls = 0;
                drawTime = time;
            }
            drawCalls++;
        }
        context.getMainWindow().frameUpdate(partialTicks);
        if (context.getMainWindow() != context.getCurrentWindow()) {
            context.getCurrentWindow().frameUpdate(partialTicks);
        }
        drawDefaultBackground();

        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        for (ModularWindow window : context.getOpenWindowsReversed()) {
            if (window.isEnabled()) {
                window.drawWidgets(partialTicks, false);
            }
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();
    }

    protected void drawGuiContainerForegroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.pushMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        Widget hovered = context.getCursor().getHovered();
        if (hovered != null && !context.getCursor().isHoldingSomething()) {
            if (hovered instanceof IVanillaSlot && ((IVanillaSlot) hovered).getMcSlot().getHasStack()) {
                renderToolTip(((IVanillaSlot) hovered).getMcSlot().getStack(), mouseX, mouseY);
            } else if (hovered.getTooltipShowUpDelay() <= context.getCursor().getTimeHovered()) {
                List<Text> tooltip = hovered.getTooltip();
                if (!tooltip.isEmpty()) {
                    GuiHelper.drawHoveringText(tooltip, context.getMousePos(), context.getScaledScreenSize(), 400, 1, false, Alignment.CenterLeft);
                }
            }
        }

        if (context.getCurrentWindow().isEnabled()) {
            context.getCurrentWindow().drawWidgets(partialTicks, true);
        }
        context.getCursor().draw(partialTicks);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableLighting();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    public void drawDebugScreen() {
        Size screenSize = context.getScaledScreenSize();
        int color = Color.rgb(180, 40, 115);
        int lineY = screenSize.height - 13;
        drawString(fontRenderer, "Mouse Pos: " + getMousePos(), 5, lineY, color);
        lineY -= 11;
        drawString(fontRenderer, "FPS: " + fps, 5, screenSize.height - 24, color);
        lineY -= 11;
        Widget hovered = context.getCursor().findHoveredWidget(true);
        if (hovered != null) {
            Size size = hovered.getSize();
            Pos2d pos = hovered.getAbsolutePos();
            IWidgetParent parent = hovered.getParent();

            drawBorder(pos.x, pos.y, size.width, size.height, color, 1f);
            drawBorder(parent.getAbsolutePos().x, parent.getAbsolutePos().y, parent.getSize().width, parent.getSize().height, Color.withAlpha(color, 0.3f), 1f);
            drawText("Pos: " + hovered.getPos(), 5, lineY, 1, color, false);
            lineY -= 11;
            drawText("Size: " + size, 5, lineY, 1, color, false);
            lineY -= 11;
            drawText("Parent: " + (parent instanceof ModularWindow ? "ModularWindow" : parent.toString()), 5, lineY, 1, color, false);
            lineY -= 11;
            drawText("Class: " + hovered, 5, lineY, 1, color, false);
        }
        color = Color.withAlpha(color, 25);
        for (int i = 5; i < screenSize.width; i += 5) {
            drawVerticalLine(i, 0, screenSize.height, color);
        }

        for (int i = 5; i < screenSize.height; i += 5) {
            drawHorizontalLine(0, screenSize.width, i, color);
        }
        drawRect(mousePos.x, mousePos.y, mousePos.x + 1, mousePos.y + 1, Color.withAlpha(Color.GREEN.normal, 0.8f));
    }

    protected void drawVanillaElements(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton guiButton : this.buttonList) {
            guiButton.drawButton(this.mc, mouseX, mouseY, partialTicks);
        }
        for (GuiLabel guiLabel : this.labelList) {
            guiLabel.drawLabel(this.mc, mouseX, mouseY);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        context.onClientTick();
        for (ModularWindow window : context.getOpenWindowsReversed()) {
            window.update();
        }
        context.getCursor().updateHovered();
        context.getCursor().onScreenUpdate();
    }

    private boolean isDoubleClick(long lastClick, long currentClick) {
        return currentClick - lastClick > 500;
    }

    @Override
    protected boolean hasClickedOutside(int p_193983_1_, int p_193983_2_, int p_193983_3_, int p_193983_4_) {
        for (ModularWindow window : context.getOpenWindows()) {
            if (Widget.isUnderMouse(new Pos2d(p_193983_1_, p_193983_2_), window.getAbsolutePos(), window.getSize())) {
                return false;
            }
        }
        return super.hasClickedOutside(p_193983_1_, p_193983_2_, p_193983_3_, p_193983_4_);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        long time = Minecraft.getSystemTime();
        boolean doubleClick = isDoubleClick(lastClick, time);
        lastClick = time;
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onClick(mouseButton, doubleClick);
        }

        if (context.getCursor().onMouseClick(mouseButton)) {
            lastFocusedClick = time;
            return;
        }

        Interactable probablyClicked = null;
        boolean wasSuccess = false;
        doubleClick = isDoubleClick(lastFocusedClick, time);
        loop:
        for (Object hovered : getCursor().getAllHovered()) {
            if (context.getCursor().onHoveredClick(mouseButton, hovered)) {
                break;
            }
            if (hovered instanceof Interactable) {
                Interactable interactable = (Interactable) hovered;
                Interactable.ClickResult result = interactable.onClick(mouseButton, doubleClick && lastClicked == interactable);
                switch (result) {
                    case IGNORE:
                        continue;
                    case ACKNOWLEDGED:
                        if (probablyClicked == null) {
                            probablyClicked = interactable;
                        }
                        continue;
                    case REJECT:
                        probablyClicked = null;
                        break loop;
                    case ACCEPT:
                        probablyClicked = interactable;
                        break loop;
                    case SUCCESS:
                        probablyClicked = interactable;
                        wasSuccess = true;
                        getCursor().updateFocused((Widget) interactable);
                        break loop;
                }
            }
        }
        this.lastClicked = probablyClicked;
        if (!wasSuccess) {
            getCursor().updateFocused(null);
        }
        if (probablyClicked == null) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        lastFocusedClick = time;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onClickReleased(mouseButton);
        }
        if ((lastClicked == null || !lastClicked.onClickReleased(mouseButton)) && !context.getCursor().onMouseRelease()) {
            super.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onMouseDragged(mouseButton, timeSinceLastClick);
        }
        if (lastClicked != null) {
            lastClicked.onMouseDragged(mouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // debug mode C + CTRL + SHIFT + ALT
        if (keyCode == 46 && isCtrlKeyDown() && isShiftKeyDown() && isAltKeyDown()) {
            debugMode = !debugMode;
        }
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onKeyPressed(typedChar, keyCode);
        }

        Widget focused = getCursor().getFocused();
        if (focused instanceof Interactable && ((Interactable) focused).onKeyPressed(typedChar, keyCode)) {
            return;
        }
        for (Object hovered : getCursor().getAllHovered()) {
            if (focused != hovered && hovered instanceof Interactable && ((Interactable) hovered).onKeyPressed(typedChar, keyCode)) {
                return;
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            this.context.tryClose();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    public void mouseScroll(int direction) {
        for (Interactable interactable : context.getCurrentWindow().getInteractionListeners()) {
            interactable.onMouseScroll(direction);
        }
        Widget focused = getCursor().getFocused();
        if (focused instanceof Interactable && ((Interactable) focused).onMouseScroll(direction)) {
            return;
        }
        for (Object hovered : getCursor().getAllHovered()) {
            if (focused != hovered && hovered instanceof Interactable && ((Interactable) hovered).onMouseScroll(direction)) {
                return;
            }
        }
    }

    public boolean isDragSplitting() {
        return dragSplitting;
    }

    public Set<Slot> getDragSlots() {
        return dragSplittingSlots;
    }

    public RenderItem getItemRenderer() {
        return itemRender;
    }

    public float getZ() {
        return zLevel;
    }

    public void setZ(float z) {
        this.zLevel = z;
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    @SideOnly(Side.CLIENT)
    public static void drawBorder(float x, float y, float width, float height, int color, float border) {
        drawSolidRect(x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(x - border, y, border, height, color);
        drawSolidRect(x + width, y, border, height, color);
    }

    @SideOnly(Side.CLIENT)
    public static void drawSolidRect(float x, float y, float width, float height, int color) {
        drawRect(x, y, x + width, y + height, color);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
    }

    @SideOnly(Side.CLIENT)
    public static void drawText(String text, float x, float y, float scale, int color, boolean shadow) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 0f);
        float sf = 1 / scale;
        fontRenderer.drawString(text, x * sf, y * sf, color, shadow);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
    }

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(r, g, b, a);
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(left, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, bottom, 0.0D).endVertex();
        bufferbuilder.pos(right, top, 0.0D).endVertex();
        bufferbuilder.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
