namespace JDH.Chapter1.Home;

class HomeAnimationLifecycleStory : StoryResource
{
    Id("jdh:chapter_1_home_animation_lifecycle");

    OnStart()
    {
        Action(
            type: "echo_npc:spawn",
            actorId: "jdh:john_father",
            definitionId: "jdh:john_father",
            worldId: "minecraft:overworld",
            x: "0",
            y: "64",
            z: "0",
            yaw: "180",
            pitch: "0",
            output: "spawn_result");
        Action(
            type: "echo_animation:play_animation",
            animation: "jdh:look_around_once",
            target: "jdh:john_father",
            await: "COMPLETED",
            output: "look_result");
        Action(
            type: "echo_animation:play_animation",
            animation: "jdh:wave",
            target: "jdh:john_father",
            await: "STARTED",
            output: "wave_start");
        Action(
            type: "echo_animation:stop_animation",
            animation: "jdh:wave",
            target: "jdh:john_father",
            output: "wave_stop");
        Action(
            type: "echo_animation:wait_animation",
            animation: "jdh:wave",
            target: "jdh:john_father",
            output: "wave_terminal");
    }
}
