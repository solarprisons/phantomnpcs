package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Appearance configuration for player NPCs.
 */
public interface PlayerAppearance extends LivingAppearance {

    /**
     * Sets the skin using texture data.
     *
     * @param skin the skin data
     */
    void setSkin(@NotNull Skin skin);

    /**
     * Gets the current skin.
     *
     * @return the skin, or null if none set
     */
    @Nullable Skin getSkin();

    /**
     * Sets the skin from a player UUID.
     * This fetches the skin asynchronously from Mojang.
     *
     * @param uuid the player UUID
     * @return future that completes when skin is applied
     */
    CompletableFuture<Void> setSkinFromUUID(@NotNull UUID uuid);

    /**
     * Sets the skin from a player name.
     * This fetches the skin asynchronously from Mojang.
     *
     * @param name the player name
     * @return future that completes when skin is applied
     */
    CompletableFuture<Void> setSkinFromName(@NotNull String name);

    /**
     * Sets the skin layer visibility.
     *
     * @param layer   the skin layer
     * @param visible whether visible
     */
    void setSkinLayerVisible(@NotNull SkinLayer layer, boolean visible);

    /**
     * Returns whether a skin layer is visible.
     *
     * @param layer the skin layer
     * @return true if visible
     */
    boolean isSkinLayerVisible(@NotNull SkinLayer layer);

    /**
     * Sets all skin layers visible.
     */
    void setAllSkinLayersVisible();

    /**
     * Skin layer parts.
     */
    enum SkinLayer {
        CAPE,
        JACKET,
        LEFT_SLEEVE,
        RIGHT_SLEEVE,
        LEFT_PANTS,
        RIGHT_PANTS,
        HAT
    }

    /**
     * Represents skin texture data.
     *
     * @param texture   the base64-encoded texture
     * @param signature the signature from Mojang
     */
    record Skin(@NotNull String texture, @Nullable String signature) {

        /**
         * Creates a skin from raw texture data.
         *
         * @param texture   base64 texture
         * @param signature optional signature
         * @return new skin
         */
        public static Skin of(@NotNull String texture, @Nullable String signature) {
            return new Skin(texture, signature);
        }

        /**
         * Creates an unsigned skin.
         *
         * @param texture base64 texture
         * @return new skin without signature
         */
        public static Skin unsigned(@NotNull String texture) {
            return new Skin(texture, null);
        }

        /**
         * Returns whether this skin is signed.
         *
         * @return true if has signature
         */
        public boolean isSigned() {
            return signature != null && !signature.isEmpty();
        }
    }
}
