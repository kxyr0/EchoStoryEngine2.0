package io.github.kxyr0.echoplayervitals.forge;

import io.github.kxyr0.echoplayervitals.forge.api.EchoPlayerVitalsServiceLookupEvent;
import io.github.kxyr0.echoplayervitals.forge.api.client.PlayerVitalsClientBridge;
import io.github.kxyr0.echoplayervitals.forge.api.lifecycle.PlayerVitalsLifecycleEvent;
import io.github.kxyr0.echoplayervitals.forge.api.lifecycle.PlayerVitalsLifecycleReason;
import io.github.kxyr0.echoplayervitals.forge.client.PlayerVitalsClientMirror;
import io.github.kxyr0.echoplayervitals.forge.command.PlayerVitalsCommandRegistration;
import io.github.kxyr0.echoplayervitals.forge.data.SavedDataPlayerVitalsRepository;
import io.github.kxyr0.echoplayervitals.forge.effect.PlayerFatigueMovementEffect;
import io.github.kxyr0.echoplayervitals.forge.effect.PlayerHungerNeutralizer;
import io.github.kxyr0.echoplayervitals.forge.sync.PlayerVitalsSyncChannel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint for Echo Player Vitals.
 */
@Mod(EchoPlayerVitalsMod.MOD_ID)
public final class EchoPlayerVitalsMod {
    public static final String MOD_ID = "echo_player_vitals";
    public static final String MOVEMENT_MODIFIER_ID = "echo_player_vitals:fatigue_movement";
    private final PlayerVitalsSyncChannel syncChannel = new PlayerVitalsSyncChannel();
    private final PlayerVitalsRuntimeState state = PlayerVitalsRuntimeState.create(this::publish);
    private final PlayerFatigueMovementEffect movementEffect = new PlayerFatigueMovementEffect();
    private final PlayerHungerNeutralizer hungerNeutralizer = new PlayerHungerNeutralizer();

    public EchoPlayerVitalsMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> this::attachClientBridge);
    }

    private void attachClientBridge() {
        PlayerVitalsClientBridge.attach(PlayerVitalsClientMirror.INSTANCE);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new PlayerVitalsCommandRegistration(state.services()).register(event.getDispatcher());
    }

    private void onServiceLookup(EchoPlayerVitalsServiceLookupEvent event) {
        state.useRepository(SavedDataPlayerVitalsRepository.open(event.server()));
        event.provide(state.services());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            state.useRepository(SavedDataPlayerVitalsRepository.open(player.getServer()));
            state.services().ensure(player, PlayerVitalsLifecycleReason.LOGIN);
            syncChannel.sendTo(player, state.services().snapshotFor(player));
        }
    }

    private void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            state.services().ensure(player, PlayerVitalsLifecycleReason.RESPAWN);
        }
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        hungerNeutralizer.neutralize(player);
        state.services().findByAccountUuid(player.getUUID())
                .ifPresent(snapshot -> movementEffect.apply(player, snapshot.fatigue()));
    }

    private void publish(PlayerVitalsPublication publication) {
        syncChannel.sendToAll(publication.snapshot());
        MinecraftForge.EVENT_BUS.post(new PlayerVitalsLifecycleEvent(publication.snapshot(), publication.reason()));
    }
}
