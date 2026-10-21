package io.github.kxyr0.echonpc.forge.command;

import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

final class EchoNpcFormCommandActions {
    private final Supplier<Optional<ForgeNpcHost>> host;

    EchoNpcFormCommandActions(Supplier<Optional<ForgeNpcHost>> host) {
        this.host = java.util.Objects.requireNonNull(host, "host");
    }

    int list(CommandContext<CommandSourceStack> context) {
        host.get().ifPresentOrElse(value -> value.formCatalog(50).forms().forEach(form -> send(context,
                form.id() + " skin=" + form.appearance().skinId().map(Object::toString).orElse("<none>")
                        + " dimensions=" + form.dimensions() + " fingerprint="
                        + form.fingerprint().substring(0, 12))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int inspect(CommandContext<CommandSourceStack> context) {
        NpcFormId formId = formId(context);
        host.get().ifPresentOrElse(value -> send(context, value.form(formId).map(form -> "form=" + form.id()
                + " source=" + form.sourceLocation() + " revision=" + form.revision()
                + " skin=" + form.appearance().skinId().map(Object::toString).orElse("<none>")
                + " model=" + form.appearance().modelProfile()
                + " bodyScale=" + form.bodyGeometry().globalScale()
                + " parts=" + form.bodyGeometry().parts()
                + " dimensions=" + form.dimensions()
                + " anchors=" + form.anchors()
                + " animationGraph=" + form.appearance().animationGraphId()
                + " faceProfile=" + form.appearance().faceProfileId()
                + " portrait=" + form.appearance().portraitResourceId()
                + " fingerprint=" + form.fingerprint()).orElse("Форма не найдена.")),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int get(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        host.get().ifPresentOrElse(value -> send(context,
                value.getForm(npcId).map(form -> "npc=" + npcId + " activeForm=" + form)
                        .orElse("Активная форма не задана.")),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int set(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        NpcFormId formId = formId(context);
        host.get().ifPresentOrElse(value -> send(context,
                EchoNpcCommandFormatting.mutation(value.setForm(new NpcFormChangeRequest(npcId, formId)))),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    int validateSpace(CommandContext<CommandSourceStack> context) {
        ActorId npcId = npcId(context);
        NpcFormId formId = formId(context);
        host.get().ifPresentOrElse(value -> send(context, value.validateFormSpace(npcId, formId)),
                () -> send(context, "Echo NPC host недоступен."));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private ActorId npcId(CommandContext<CommandSourceStack> context) {
        return new ActorId(ResourceLocationArgument.getId(context, "npcId").toString());
    }

    private NpcFormId formId(CommandContext<CommandSourceStack> context) {
        return new NpcFormId(ResourceLocationArgument.getId(context, "formId").toString());
    }

    private void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
