package io.github.kxyr0.echodamagenumbers;

import io.github.kxyr0.echodamagenumbers.client.DamageNumberClient;
import io.github.kxyr0.echodamagenumbers.network.DamageNumberNetwork;
import io.github.kxyr0.echodamagenumbers.server.DamageNumberServerEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoDamageNumbersMod.MOD_ID)
public final class EchoDamageNumbersMod {
    public static final String MOD_ID = "echo_damage_numbers";

    public EchoDamageNumbersMod() {
        DamageNumberNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, DamageNumberServerEvents::onDamage);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> DamageNumberClient::register);
    }
}

