package ac.boar.injector.provider;

import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.extension.GeyserExtensionLoader;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

public class GeyserExtensionClassProvider implements IClassProvider {
    @Override
    @Nonnull
    public byte[] getClass(@NotNull String name) throws ClassNotFoundException {
        Class<?> klass = ((GeyserExtensionLoader)GeyserImpl.getInstance().getExtensionManager().extensionLoader()).classByName(name);

        try (InputStream is = klass.getClassLoader().getResourceAsStream(slash(name) + ".class")) {
            Objects.requireNonNull(is, "Class input stream is null");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
            return baos.toByteArray();
        } catch (Throwable t) {
            throw new ClassNotFoundException(name, t);
        }
    }

    @Override
    @Nonnull
    public Map<String, Supplier<byte[]>> getAllClasses() {
        return Map.of();
    }
}
