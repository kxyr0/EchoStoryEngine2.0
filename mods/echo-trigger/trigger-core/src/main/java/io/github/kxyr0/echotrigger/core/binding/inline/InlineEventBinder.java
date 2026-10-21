package io.github.kxyr0.echotrigger.core.binding.inline;

import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;
import io.github.kxyr0.echotrigger.core.binding.TriggerCallSyntax;
import io.github.kxyr0.echotrigger.core.binding.domain.EventActionBinder;
import io.github.kxyr0.echotrigger.core.binding.domain.EventBindingException;
import io.github.kxyr0.echotrigger.core.binding.domain.EventPredicateBinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class InlineEventBinder {
    public boolean hasDeclarations(ClassSyntax resource) {
        return resource.members().stream().anyMatch(member ->
                member instanceof ClassSectionSyntax section
                        && TriggerCallSyntax.qualifiedName(section.invocation()).equals("When")
                || member instanceof ClassCallSyntax call
                        && TriggerCallSyntax.qualifiedName(call.invocation()).equals("When"));
    }

    public InlineEventResult bind(CompilationUnitSyntax unit, String revision) {
        var resource = unit.classDeclaration();
        var sections = resource.members().stream().filter(ClassSectionSyntax.class::isInstance)
                .map(ClassSectionSyntax.class::cast)
                .filter(section -> TriggerCallSyntax.qualifiedName(section.invocation()).equals("When")).toList();
        List<EventTriggerDefinition> definitions = new ArrayList<>();
        List<TriggerDiagnostic> diagnostics = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        resource.members().stream().filter(ClassCallSyntax.class::isInstance).map(ClassCallSyntax.class::cast)
                .filter(call -> TriggerCallSyntax.qualifiedName(call.invocation()).equals("When"))
                .forEach(call -> diagnostics.add(problem(call.span(), new IllegalArgumentException(
                        "Inline When требует тело { actions; }"))));
        for (var section : sections) {
            try {
                String profile = resource.baseType().name().text();
                if (!Set.of("StoryResource", "DialogueResource", "TriggerResource").contains(profile)) {
                    throw new IllegalArgumentException("When поддерживается в Story, Dialogue и TriggerResource");
                }
                String owner = InlineResourceIdentity.resolve(unit.span().sourceId().value(), resource);
                var definition = bindSection(section, owner, profile, revision);
                if (!ids.add(definition.subscription().id().value())) {
                    throw new IllegalArgumentException("Повторный id блока When в ресурсе");
                }
                definitions.add(definition);
            } catch (IllegalArgumentException failure) {
                diagnostics.add(problem(section.invocation().span(), failure));
            }
        }
        return new InlineEventResult(diagnostics.isEmpty() ? definitions : List.of(), diagnostics);
    }

    private EventTriggerDefinition bindSection(ClassSectionSyntax section, String owner, String profile,
            String revision) {
        var call = section.invocation();
        validateArguments(section);
        String id = TriggerCallSyntax.namedText(call, "id")
                .orElseThrow(() -> new IllegalArgumentException("When требует стабильный id: \"after_father\""));
        if (!id.matches("[a-z0-9_/-]+")) {
            throw new IllegalArgumentException("id блока When: допустимы a-z, 0-9, _, / и -");
        }
        new ResourceId(owner);
        if (owner.contains("#")) {
            throw new IllegalArgumentException("Id владельца When не может содержать #point");
        }
        String subscriptionId = owner.substring(0, owner.indexOf(':') + 1) + "__events/" + profile + "/"
                + owner.substring(owner.indexOf(':') + 1) + "/" + id;
        subscriptionId = subscriptionId.replace("Resource", "").toLowerCase(java.util.Locale.ROOT);
        var selector = TriggerCallSyntax.positionalInvocation(call, 0)
                .orElseThrow(() -> new IllegalArgumentException("When требует typed predicate первым аргументом"));
        var filter = new EventPredicateBinder().bind(selector, name -> Optional.empty());
        boolean once = call.arguments().stream().filter(arg -> arg.name().filter("once"::equals).isPresent())
                .map(arg -> ((BooleanLiteralExpressionSyntax) arg.value()).value()).findFirst().orElse(false);
        var subscription = new EventSubscription(new EventSubscriptionId(subscriptionId + "#When"),
                filter, revision, once);
        var span = selector.span();
        return new EventTriggerDefinition(subscription, new EventTriggerDefinition.Location(
                span.sourceId().value(), span.start().line(), span.start().column()),
                new EventActionBinder().bind(section.body(), name -> Optional.empty()));
    }

    private void validateArguments(ClassSectionSyntax section) {
        var args = section.invocation().arguments();
        if (args.isEmpty() || args.get(0).name().isPresent()) {
            throw new IllegalArgumentException("When требует typed predicate первым аргументом");
        }
        Set<String> named = new HashSet<>();
        for (var arg : args.subList(1, args.size())) {
            String name = arg.name().orElse("");
            if (!Set.of("id", "once").contains(name) || !named.add(name)) {
                throw new IllegalArgumentException("When поддерживает только уникальные id и once");
            }
            validateNamed(arg);

        }
    }

    private void validateNamed(ArgumentSyntax arg) {
        if (arg.name().filter("once"::equals).isPresent() && !(arg.value() instanceof BooleanLiteralExpressionSyntax)) {
            throw new IllegalArgumentException("once требует true или false");
        }
        if (arg.name().filter("id"::equals).isPresent() && !(arg.value() instanceof StringLiteralExpressionSyntax)) {
            throw new IllegalArgumentException("id требует строковый литерал");
        }
    }

    private TriggerDiagnostic problem(SourceSpan span, IllegalArgumentException failure) {
        var location = failure instanceof EventBindingException located ? located.source()
                : new EventTriggerDefinition.Location(span.sourceId().value(), span.start().line(),
                        span.start().column());
        return new TriggerDiagnostic(TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID,
                failure.getMessage(), Optional.of(location));
    }
}
