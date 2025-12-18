package prisons.solar.npclib.core.config;

import io.github.wasabithumb.jtoml.JToml;
import io.github.wasabithumb.jtoml.document.TomlDocument;
import io.github.wasabithumb.jtoml.key.TomlKey;
import io.github.wasabithumb.jtoml.value.TomlValue;
import io.github.wasabithumb.jtoml.value.primitive.TomlPrimitive;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.config.PhantomConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class TomlConfigLoader {

    private final String defaultResource;

    public TomlConfigLoader(@NotNull String defaultResource) {
        this.defaultResource = defaultResource;
    }

    @NotNull
    public PhantomConfig loadPhantomConfig(@NotNull Path path) throws IOException {
        return parse(load(path));
    }

    @NotNull
    public TomlDocument load(@NotNull Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            try (InputStream in = getClass().getResourceAsStream(defaultResource)) {
                if (in == null) throw new IOException("Missing resource: " + defaultResource);
                Files.copy(in, path);
            }
        }
        return JToml.jToml().read(path);
    }

    private PhantomConfig parse(TomlDocument doc) {
        PhantomConfig.Builder b = PhantomConfig.builder();

        read(doc, "general.view-distance", TomlPrimitive::asDouble, b::viewDistance);
        read(doc, "general.debug", TomlPrimitive::asBoolean, b::debug);
        read(doc, "tick-rates.visibility", TomlPrimitive::asInteger, b::visibilityTickRate);
        read(doc, "tick-rates.ai", TomlPrimitive::asInteger, b::aiTickRate);
        read(doc, "tick-rates.proximity", TomlPrimitive::asInteger, b::proximityTickRate);
        read(doc, "performance.async-visibility", TomlPrimitive::asBoolean, b::asyncVisibility);
        read(doc, "performance.packet-batching", TomlPrimitive::asBoolean, b::packetBatching);
        read(doc, "performance.packet-batch-size", TomlPrimitive::asInteger, b::packetBatchSize);
        read(doc, "performance.packet-batch-flush-interval", TomlPrimitive::asInteger, b::packetBatchFlushInterval);
        read(doc, "skin.cache-duration", TomlPrimitive::asInteger, b::skinCacheDuration);

        return b.build();
    }

    private <T> void read(TomlDocument doc, String key, Function<TomlPrimitive, T> parser, Consumer<T> setter) {
        try {
            TomlValue value = doc.get(TomlKey.parse(key));
            if (value != null && value.isPrimitive()) {
                setter.accept(parser.apply(value.asPrimitive()));
            }
        } catch (Exception ignored) {
        }
    }
}
