package ac.boar.injector.transformers;

import net.lenni0451.classtransform.annotations.CInline;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import org.cloudburstmc.netty.channel.raknet.packet.RakDatagramPacket;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;

@CTransformer(RakSessionCodec.class)
public class RakSessionCodecTransformer {
    @CInline
    @CInject(method = "onIncomingAck", target = @CTarget("HEAD"))
    public void onIncomingAck(RakDatagramPacket datagram, long curTime) {
        System.out.println("Acks: " + curTime);
    }
}
