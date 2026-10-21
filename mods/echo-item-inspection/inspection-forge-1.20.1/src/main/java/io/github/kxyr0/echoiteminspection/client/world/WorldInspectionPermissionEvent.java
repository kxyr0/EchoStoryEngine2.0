package io.github.kxyr0.echoiteminspection.client.world;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/** Foreground systems may veto world inspection without taking a dependency on IPLA. */
@Cancelable
public final class WorldInspectionPermissionEvent extends Event { }
