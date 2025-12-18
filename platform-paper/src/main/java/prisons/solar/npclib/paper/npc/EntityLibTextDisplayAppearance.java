package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.TextDisplayAppearance;

/**
 * EntityLib implementation of text display appearance.
 */
public class EntityLibTextDisplayAppearance extends AbstractEntityLibDisplayAppearance<EntityLibTextDisplayNPC>
        implements TextDisplayAppearance {

    private String text = "";
    private int lineWidth = 200;
    private int backgroundColor = 0x40000000; // Default semi-transparent black
    private int textOpacity = 255;
    private boolean shadow = false;
    private boolean seeThrough = false;
    private boolean defaultBackground = false;
    private TextAlignment alignment = TextAlignment.CENTER;

    public EntityLibTextDisplayAppearance(@NotNull EntityLibTextDisplayNPC npc) {
        super(npc);
    }

    @Override
    public void setText(@NotNull String text) {
        this.text = text;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull String getText() {
        return text;
    }

    @Override
    public void setLineWidth(int width) {
        this.lineWidth = width;
        npc.onMetadataChanged();
    }

    @Override
    public int getLineWidth() {
        return lineWidth;
    }

    @Override
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        npc.onMetadataChanged();
    }

    @Override
    public int getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setTextOpacity(int opacity) {
        this.textOpacity = Math.max(0, Math.min(255, opacity));
        npc.onMetadataChanged();
    }

    @Override
    public int getTextOpacity() {
        return textOpacity;
    }

    @Override
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
        npc.onMetadataChanged();
    }

    @Override
    public boolean hasShadow() {
        return shadow;
    }

    @Override
    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isSeeThrough() {
        return seeThrough;
    }

    @Override
    public void setDefaultBackground(boolean useDefault) {
        this.defaultBackground = useDefault;
        npc.onMetadataChanged();
    }

    @Override
    public boolean hasDefaultBackground() {
        return defaultBackground;
    }

    @Override
    public void setAlignment(@NotNull TextAlignment alignment) {
        this.alignment = alignment;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull TextAlignment getAlignment() {
        return alignment;
    }
}
