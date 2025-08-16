package ac.boar.injector;

import ac.boar.injector.transformers.RakSessionCodecTransformer;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.reflect.Agents;

import java.io.IOException;

public class BoarInjector {
    public static void injectToRak() {
        TransformerManager transformerManager = new TransformerManager(new BasicClassProvider());
        transformerManager.addTransformer(RakSessionCodecTransformer.class.getName());
        try {
            transformerManager.hookInstrumentation(Agents.getInstrumentation());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
