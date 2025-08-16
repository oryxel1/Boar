package ac.boar.injector.transformers;

import net.lenni0451.classtransform.annotations.CInline;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import org.cloudburstmc.netty.channel.raknet.packet.RakDatagramPacket;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.ExtensionLoader;
import org.geysermc.geyser.extension.GeyserExtensionLoader;

@SuppressWarnings("ALL")
@CTransformer(RakSessionCodec.class)
public class RakSessionCodecTransformer {
    @CInline
    @CInject(method = "onIncomingAck", target = @CTarget("HEAD"))
    public void onIncomingAck(RakDatagramPacket datagram, long curTime) {
        ExtensionLoader extLoader = GeyserImpl.getInstance().getExtensionManager().extensionLoader();
        if (!(extLoader instanceof GeyserExtensionLoader extensionLoader)) {
            return;
        }

        try {
            Class<?> klass = extensionLoader.classByName("ac.boar.anticheat.acks.BoarAcknowledgement");
            klass.getDeclaredMethod("handle", RakSessionCodec.class, RakDatagramPacket.class).invoke(null, ((Object) this), datagram);
        } catch (Exception ignored) {
            return;
        }
    }
}
