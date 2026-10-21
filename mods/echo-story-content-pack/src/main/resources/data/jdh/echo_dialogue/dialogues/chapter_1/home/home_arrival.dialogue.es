namespace JDH.Chapter1.Home;

class HomeArrivalDialogue : DialogueResource
{
    private NodeDef[] dialogueNodes = [ Greeting(), ToldTruth(), Lied() ];
    private FaceAnimationId PlayerLookLeft = "jdh:player/look_left";
    private FaceAnimationId PlayerBrowsRaise = "jdh:player/brows_raise";
    private FaceAnimationId FatherAngryBrows = "jdh:john_father/angry_brows";
    private FaceAnimationId FatherBrowsRaise = "jdh:john_father/brows_raise";
    private FaceAnimationId FatherFullFaceHold = "jdh:john_father/full_face_hold";

    public override DialogueResourceDef Build()
    {
        return Dialogue("jdh:chapter_1_home_arrival");
    }

    private void Init()
    {
        Id("jdh:chapter_1_home_arrival");
        Start("greeting");
        Alias("father", "jdh:john_father", display: "Отец", visual: Portrait("jdh:john_father"));
        Alias("john", "jdh:john", display: "Лололошка", visual: SkinHead("echo_content_player:player/base"));
    }

    private NodeDef[] Nodes()
    {
        return dialogueNodes;
    }

    private NodeDef Greeting()
    {
        var node = new Node("greeting");
        Line("father", "Ты поздно вернулся.").SetAnimationFace(FatherAngryBrows);
        Line("father", "Я ждал почти весь вечер.").StopFaceAnimation();
        Line("john", "Я знаю.").SetAnimationFace(PlayerLookLeft);
        Line("john", "Я не хотел будить дом.").StopFaceAnimation();
        Line("father", "Скажи прямо, где ты был?").SetAnimationFace(FatherBrowsRaise);
        Line("father", "Без красивых историй.").StopFaceAnimation();
        Line("john", "Я могу объяснить.").SetAnimationFaceClear();
        Choice("tell_truth", "Сказать правду.", next: "told_truth");
        Choice("lie", "Солгать.", next: "lied");
        return node;
    }

    private NodeDef ToldTruth()
    {
        var node = new Node("told_truth");
        Line("john", "Я был у старого моста.").SetAnimationFace(PlayerLookLeft);
        Line("john", "У воды было тихо.").StopFaceAnimation();
        Line("father", "Один?").SetAnimationFace(FatherFullFaceHold);
        Line("father", "Ты понимаешь, как это звучит?").StopFaceAnimation();
        Line("john", "Да. Просто хотел побыть в тишине.").SetAnimationFaceClear();
        Line("father", "Тогда утром поговорим спокойно.").SetAnimationFace(FatherBrowsRaise);
        Line("father", "Сейчас иди домой.").StopFaceAnimation();
        Line("john", "Хорошо.").SetAnimationFaceClear();
        Complete("jdh:told_truth");
        return node;
    }

    private NodeDef Lied()
    {
        var node = new Node("lied");
        Line("john", "Я просто задержался во дворе.").SetAnimationFace(PlayerLookLeft);
        Line("john", "Я не думал, что будет так поздно.").StopFaceAnimation();
        Line("father", "Не во дворе.").SetAnimationFace(FatherAngryBrows);
        Line("father", "Я видел следы у дороги.").StopFaceAnimation();
        Line("john", "Почему ты так уверен?").SetAnimationFace(PlayerBrowsRaise);
        Line("john", "Я правда хотел быстро вернуться.").StopFaceAnimation();
        Line("father", "Я вижу, когда ты недоговариваешь.").SetAnimationFace(FatherFullFaceHold);
        Line("father", "И сейчас это видно слишком хорошо.").StopFaceAnimation();
        Line("john", "Понял.").SetAnimationFaceClear();
        Complete("jdh:lied");
        return node;
    }
}
