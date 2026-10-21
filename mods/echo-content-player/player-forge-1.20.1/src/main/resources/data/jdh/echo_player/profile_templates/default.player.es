namespace EchoContentPlayer.Jdh;

class DefaultPlayerProfile : PlayerProfileTemplate
{
    public override PlayerProfile Build()
    {
        return PlayerProfile(
            id: "jdh:player/default",
            nameMode: CUSTOM,
            displayName: "Лололошка",
            spark: 100,
            maxSpark: 100,
            skinMode: RESOURCE,
            skinId: "echo_content_player:player/base",
            skinModel: CLASSIC
        );
    }
}
