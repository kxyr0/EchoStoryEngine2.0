package io.github.kxyr0.echomagic.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MagicCommands {
    private final ForgeMagicServices services;
    public MagicCommands(ForgeMagicServices services) { this.services = services; }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_magic").requires(source -> source.hasPermission(2))
                .then(playerSpell("grant", this::grant))
                .then(playerSpell("remove", this::remove))
                .then(playerSpell("select", this::select))
                .then(Commands.literal("cast").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> cast(context.getSource(), EntityArgument.getPlayer(context, "player"), Optional.empty()))
                        .then(Commands.argument("spell", ResourceLocationArgument.id()).executes(context -> cast(
                                context.getSource(), EntityArgument.getPlayer(context, "player"),
                                Optional.of(spell(context, "spell")))))))
                .then(Commands.literal("status").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("spells").executes(context -> spells(context.getSource())))
                .then(debugTree()));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> playerSpell(
            String name, PlayerSpellAction action) {
        return Commands.literal(name).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("spell", ResourceLocationArgument.id()).executes(context -> action.run(
                        context.getSource(), EntityArgument.getPlayer(context, "player"), spell(context, "spell")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> debugTree() {
        return Commands.literal("debug")
                .then(Commands.literal("player").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("projectiles").then(Commands.literal("on").executes(context -> message(
                        context.getSource(), "Projectile debug enabled; use the F3 Echo Magic overlay.")))
                        .then(Commands.literal("off").executes(context -> message(context.getSource(), "Projectile debug disabled."))))
                .then(Commands.literal("status").executes(context -> message(context.getSource(),
                        "active casts=" + services.activeCastCount() + ", projectiles=" + services.activeProjectileCount()
                                + ", diagnostics=" + services.diagnostics().size())));
    }

    private int grant(CommandSourceStack source, ServerPlayer player, SpellId spell) {
        boolean changed = services.grant(ref(player), spell);
        return message(source, changed ? "Granted " + spell : "Spell was already known or is unavailable: " + spell);
    }
    private int remove(CommandSourceStack source, ServerPlayer player, SpellId spell) {
        boolean changed = services.remove(ref(player), spell);
        return message(source, changed ? "Removed " + spell : "Spell was not known: " + spell);
    }
    private int select(CommandSourceStack source, ServerPlayer player, SpellId spell) {
        boolean changed = services.select(ref(player), spell);
        return message(source, changed ? "Selected " + spell : "Cannot select unknown spell: " + spell);
    }
    private int cast(CommandSourceStack source, ServerPlayer player, Optional<SpellId> spell) {
        SpellCastCommandId command = new SpellCastCommandId("command:" + player.getUUID() + ':'
                + source.getLevel().getGameTime() + ':' + spell.map(SpellId::value).orElse("selected"));
        var receipt = services.cast(ref(player), spell, command);
        return message(source, receipt.accepted() ? "Cast accepted: " + receipt.castId().orElseThrow()
                : "Cast rejected: " + receipt.outcome().orElseThrow());
    }
    private int status(CommandSourceStack source, ServerPlayer player) {
        MagicPlayerRef ref = ref(player);
        String selected = services.selectedSpell(ref).selected().map(SpellId::value).orElse("<none>");
        long cooldown = services.selectedSpell(ref).selected().map(id -> services.cooldown(ref, id).remainingTicks()).orElse(0L);
        return message(source, "player=" + player.getScoreboardName() + ", selected=" + selected
                + ", known=" + services.knownSpells(ref).spells().size() + ", cooldown=" + cooldown);
    }
    private int spells(CommandSourceStack source) {
        return message(source, services.catalog().definitions().stream()
                .map(definition -> definition.id() + " (" + definition.displayName() + ')').toList().toString());
    }
    private int message(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
    private SpellId spell(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, String name) {
        return new SpellId(ResourceLocationArgument.getId(context, name).toString());
    }
    private MagicPlayerRef ref(ServerPlayer player) { return new MagicPlayerRef(player.getUUID()); }
    @FunctionalInterface private interface PlayerSpellAction {
        int run(CommandSourceStack source, ServerPlayer player, SpellId spell);
    }
}
