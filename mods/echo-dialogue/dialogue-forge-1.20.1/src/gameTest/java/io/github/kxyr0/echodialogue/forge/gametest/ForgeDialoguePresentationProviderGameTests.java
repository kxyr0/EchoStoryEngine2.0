package io.github.kxyr0.echodialogue.forge.gametest;

import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.assertCategory;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationCatalog;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationProviderView;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationRegistration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoDialogueMod.MOD_ID)
public final class ForgeDialoguePresentationProviderGameTests {
    static final String REGISTRATION_TEST =
            "ForgeDialoguePresentationProviderGameTests.providerJarRegistersThroughPublicEvent";
    static final String LINE_TEST =
            "ForgeDialoguePresentationProviderGameTests.dynamicLinePresentationUsesProvider";
    static final String CHOICE_TEST =
            "ForgeDialoguePresentationProviderGameTests.dynamicChoicePresentationKeepsChoices";
    static final String FALLBACK_TEST =
            "ForgeDialoguePresentationProviderGameTests.providerFallbackScenariosUseAuthoredPresentation";
    static final String AMBIGUITY_TEST =
            "ForgeDialoguePresentationProviderGameTests.ambiguousProvidersUseAuthoredFallback";
    static final String CLOSE_TEST =
            "ForgeDialoguePresentationProviderGameTests.providerSessionsCloseWithCatalog";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeDialoguePresentationProviderGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void providerJarRegistersThroughPublicEvent(GameTestHelper helper) {
        mark(REGISTRATION_TEST);
        ForgeSpeakerPresentationCatalog catalog = registerCatalog(helper, new InMemoryDiagnosticBuffer(32));
        Map<String, ForgeSpeakerPresentationProviderView> views = views(catalog);
        helper.assertTrue(views.containsKey("echo_dialogue_test:main_provider"), "main provider must register");
        helper.assertTrue(views.containsKey("echo_dialogue_test:ambiguity_provider"),
                "ambiguity provider must register");
        helper.assertTrue(views.values().stream().allMatch(view ->
                view.status() == ForgeSpeakerPresentationProviderView.Status.ACTIVE), "providers must be active");
        catalog.close();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void dynamicLinePresentationUsesProvider(GameTestHelper helper) {
        mark(LINE_TEST);
        ForgeSpeakerPresentationCatalog catalog = registerCatalog(helper, new InMemoryDiagnosticBuffer(32));
        DialoguePresentation result = catalog.resolve(line("echo_dialogue_test:dynamic_father"));
        helper.assertTrue(result.speakerDisplayName().equals("Динамический Отец"),
                "dynamic name must replace authored");
        helper.assertTrue(result.speakerVisual().orElseThrow().id().toString()
                .equals("echo_dialogue_test:dynamic_father"), "dynamic visual must replace authored");
        helper.assertTrue(result.text().equals("Исходный текст."), "line text must stay authored");
        helper.assertTrue(result.revision() == 5, "revision must not change");
        catalog.close();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void dynamicChoicePresentationKeepsChoices(GameTestHelper helper) {
        mark(CHOICE_TEST);
        ForgeSpeakerPresentationCatalog catalog = registerCatalog(helper, new InMemoryDiagnosticBuffer(32));
        DialoguePresentation result = catalog.resolve(choice("echo_dialogue_test:dynamic_father"));
        helper.assertTrue(result.kind() == DialoguePresentationKind.CHOICE, "choice kind must stay");
        helper.assertTrue(result.speakerDisplayName().equals("Динамический Отец"), "choice speaker must resolve");
        helper.assertTrue(result.choices().stream().map(choice -> choice.id().toString()).toList()
                .equals(List.of("one", "two", "three", "four")), "choice order must stay");
        helper.assertTrue(result.revision() == 6, "choice revision must not change");
        catalog.close();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void providerFallbackScenariosUseAuthoredPresentation(GameTestHelper helper) {
        mark(FALLBACK_TEST);
        ForgeSpeakerPresentationCatalog catalog = registerCatalog(helper, new InMemoryDiagnosticBuffer(64));
        for (String speakerId : List.of("echo_dialogue_test:not_handled", "echo_dialogue_test:unavailable",
                "echo_dialogue_test:failure", "echo_dialogue_test:exception", "echo_dialogue_test:malformed")) {
            DialoguePresentation result = catalog.resolve(line(speakerId));
            helper.assertTrue(result.speakerDisplayName().equals("Отец"), "authored fallback must be used");
            helper.assertTrue(result.text().equals("Исходный текст."), "fallback must keep text");
        }
        catalog.close();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void ambiguousProvidersUseAuthoredFallback(GameTestHelper helper) {
        mark(AMBIGUITY_TEST);
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(64);
        ForgeSpeakerPresentationCatalog catalog = registerCatalog(helper, buffer);
        DialoguePresentation result = catalog.resolve(line("echo_dialogue_test:ambiguous"));
        helper.assertTrue(result.speakerDisplayName().equals("Отец"), "ambiguity must use authored fallback");
        assertCategory(helper, buffer.recent(), DiagnosticCategory.PROVIDER_AMBIGUITY);
        catalog.close();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void providerSessionsCloseWithCatalog(GameTestHelper helper) {
        mark(CLOSE_TEST);
        ForgeSpeakerPresentationCatalog catalog = registerCatalog(helper, new InMemoryDiagnosticBuffer(32));
        catalog.close();
        catalog.close();
        helper.assertTrue(catalog.views().stream().allMatch(view ->
                view.status() == ForgeSpeakerPresentationProviderView.Status.STOPPED), "providers must stop once");
        helper.succeed();
    }

    private static ForgeSpeakerPresentationCatalog registerCatalog(
            GameTestHelper helper,
            InMemoryDiagnosticBuffer buffer
    ) {
        return ForgeSpeakerPresentationRegistration.register(
                helper.getLevel().getServer(), new ForgeDiagnosticSink(buffer));
    }

    private static Map<String, ForgeSpeakerPresentationProviderView> views(ForgeSpeakerPresentationCatalog catalog) {
        return catalog.views().stream().collect(Collectors.toMap(
                view -> view.descriptor().id().toString(), Function.identity()));
    }

    private static DialoguePresentation line(String speakerId) {
        DialoguePresentation.Prompt prompt = new DialoguePresentation.Prompt(
                SpeakerId.parse(speakerId), "Отец", Optional.empty(), "Исходный текст.");
        return DialoguePresentation.line(context(), 5, prompt, true);
    }

    private static DialoguePresentation choice(String speakerId) {
        DialoguePresentation.Prompt prompt = new DialoguePresentation.Prompt(
                SpeakerId.parse(speakerId), "Отец", Optional.empty(), "Выбор.");
        return DialoguePresentation.choice(
                context(),
                6,
                prompt,
                List.of(
                        new DialoguePresentation.VisibleChoice(new ChoiceId("one"), "Первый"),
                        new DialoguePresentation.VisibleChoice(new ChoiceId("two"), "Второй"),
                        new DialoguePresentation.VisibleChoice(new ChoiceId("three"), "Третий"),
                        new DialoguePresentation.VisibleChoice(new ChoiceId("four"), "Четвёртый")));
    }

    private static DialoguePresentation.Context context() {
        return new DialoguePresentation.Context(
                new DialogueSessionId("d6-session"),
                DialogueId.parse("echo_dialogue_test:dynamic_provider_showcase"),
                new AudienceId("d6-audience"));
    }

    private static void mark(String testName) {
        LOGGER.info("EDLG-GAMETEST-RUN {}", testName);
    }
}
