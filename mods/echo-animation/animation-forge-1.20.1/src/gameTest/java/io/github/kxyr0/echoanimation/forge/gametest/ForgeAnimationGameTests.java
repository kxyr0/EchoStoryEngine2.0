package io.github.kxyr0.echoanimation.forge.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.echoscript.EchoScriptAnimationCompiler;
import io.github.kxyr0.echoanimation.forge.EchoAnimationMod;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupEvent;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogCandidate;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceCandidateCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceText;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoAnimationMod.MOD_ID)
public final class ForgeAnimationGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeAnimationGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void echoAnimationCommandsExist(GameTestHelper helper) {
        mark("ForgeAnimationGameTests.echoAnimationCommandsExist");
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer()
                .getCommands()
                .getDispatcher();
        helper.assertTrue(dispatcher.getRoot().getChild("echo_animation") != null,
                "echo_animation command root must be registered");
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withPermission(4);
        List<String> commands = List.of(
                "echo_animation health",
                "echo_animation catalog",
                "echo_animation face_rigs",
                "echo_animation face_rig sample:father",
                "echo_animation face_poses",
                "echo_animation face_animations",
                "echo_animation body_rigs",
                "echo_animation body_clips",
                "echo_animation acceptance state jdh:john acting",
                "echo_animation acceptance state jdh:john wave",
                "echo_animation acceptance state jdh:john head",
                "echo_animation acceptance state jdh:john body",
                "echo_animation acceptance state jdh:john left_arm",
                "echo_animation acceptance state jdh:john right_arm",
                "echo_animation acceptance state jdh:john both_arms",
                "echo_animation acceptance state jdh:john left_leg",
                "echo_animation acceptance state jdh:john right_leg",
                "echo_animation acceptance state jdh:john step",
                "echo_animation acceptance remove jdh:john",
                "echo_animation diagnostics",
                "echo_animation diagnostics 5");
        for (String command : commands) {
            ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, source);
            helper.assertTrue(parsed.getReader().getRemaining().isEmpty(), command + " must parse");
            helper.assertTrue(parsed.getExceptions().isEmpty(), command + " must not report parse exceptions");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void faceRigResourceCompilesInForgeRuntime(GameTestHelper helper) {
        mark("ForgeAnimationGameTests.faceRigResourceCompilesInForgeRuntime");
        var result = new EchoScriptAnimationCompiler().compile("sample:father.face-rig.es",
                ForgeAnimationGameTestSources.faceRig());
        helper.assertTrue(result.faceRig().isPresent(), "valid FaceRigResource must compile");
        helper.assertTrue(result.faceRig().orElseThrow().id().equals(FaceRigId.parse("sample:father")),
                "FaceRigId must be stable");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void facePoseResourceCompilesInForgeRuntime(GameTestHelper helper) {
        mark("ForgeAnimationGameTests.facePoseResourceCompilesInForgeRuntime");
        var result = new EchoScriptAnimationCompiler().compile("sample:father_neutral.face-pose.es",
                ForgeAnimationGameTestSources.facePose());
        helper.assertTrue(result.facePose().isPresent(), "valid FacePoseResource must compile");
        helper.assertTrue(result.facePose().orElseThrow().controlDeltas().size() == 1,
                "Russian-safe pose control delta must compile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void facialAnimationResourceCompilesInForgeRuntime(GameTestHelper helper) {
        mark("ForgeAnimationGameTests.facialAnimationResourceCompilesInForgeRuntime");
        var result = new EchoScriptAnimationCompiler().compile("sample:blink.face-animation.es",
                ForgeAnimationGameTestSources.facialAnimation());
        helper.assertTrue(result.facialAnimation().isPresent(), "valid FacialAnimationResource must compile");
        helper.assertTrue(result.facialAnimation().orElseThrow().tracks().get(0).keyframes().size() == 2,
                "animation track keyframes must compile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidCandidateRetainsCatalog(GameTestHelper helper) {
        mark("ForgeAnimationGameTests.invalidCandidateRetainsCatalog");
        AnimationCatalogState state = new AnimationCatalogState();
        AnimationCatalogCandidate valid = candidate(ForgeAnimationGameTestSources.faceRig());
        state.install(valid);
        AnimationCatalogCandidate invalid = candidate(ForgeAnimationGameTestSources.invalidRig());
        state.install(invalid);
        helper.assertTrue(!invalid.valid(), "invalid candidate must be rejected");
        helper.assertTrue(state.current().faceRigs().size() == 1, "known-good catalog must remain installed");
        helper.assertTrue(state.current().faceRigs().get(0).id().equals(FaceRigId.parse("sample:father")),
                "known-good FaceRig must remain visible");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void serviceLookupResolvesCatalog(GameTestHelper helper) {
        mark("ForgeAnimationGameTests.serviceLookupResolvesCatalog");
        AnimationServiceLookupEvent event = new AnimationServiceLookupEvent(helper.getLevel().getServer());
        MinecraftForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.providers().isEmpty(), "service lookup must resolve a provider");
        helper.succeed();
    }

    private static AnimationCatalogCandidate candidate(String source) {
        AnimationResourceCandidateCompiler compiler =
                new AnimationResourceCandidateCompiler(new EchoScriptAnimationCompiler());
        return compiler.compile(List.of(new AnimationResourceText("sample:test.face-rig.es", source)));
    }

    private static void mark(String testName) {
        LOGGER.info("EANIM-GAMETEST-RUN {}", testName);
    }
}
