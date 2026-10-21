package io.github.kxyr0.echodevscenarios.forge.network;

import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.forge.client.ScenarioLauncherScreen;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ScenarioNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_dev_scenarios", "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ScenarioNetwork() { }

    public static void register() {
        CHANNEL.messageBuilder(OpenScenarioScreenMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenScenarioScreenMessage::encode).decoder(OpenScenarioScreenMessage::decode)
                .consumerMainThread(ScenarioNetwork::handle).add();
    }

    public static void open(ServerPlayer player, List<ScenarioDefinition> definitions) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenScenarioScreenMessage.from(definitions));
    }

    private static void handle(OpenScenarioScreenMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ScenarioLauncherScreen.open(message));
        context.setPacketHandled(true);
    }
}
