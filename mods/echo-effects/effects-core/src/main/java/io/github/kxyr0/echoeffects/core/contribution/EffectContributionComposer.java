package io.github.kxyr0.echoeffects.core.contribution;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelContribution;
import io.github.kxyr0.echoeffects.api.channel.EffectChannelId;
import java.util.List;

public final class EffectContributionComposer {
    public EffectContributionSnapshot compose(List<EffectChannelContribution> contributions) {
        double speed = 1.0D;
        double fov = 0.0D;
        double roll = 0.0D;
        double shake = 0.0D;
        double tint = 0.0D;
        double vignette = 0.0D;
        for (EffectChannelContribution contribution : contributions) {
            EffectChannelId channel = contribution.channelId();
            if (EffectChannelId.MOVEMENT_SPEED.equals(channel)) {
                speed *= contribution.value();
            } else if (EffectChannelId.CAMERA_FOV.equals(channel)) {
                fov += contribution.value();
            } else if (EffectChannelId.CAMERA_ROLL.equals(channel)) {
                roll += contribution.value();
            } else if (EffectChannelId.CAMERA_SHAKE.equals(channel)) {
                shake = Math.max(shake, Math.abs(contribution.value()));
            } else if (EffectChannelId.SCREEN_TINT.equals(channel)) {
                tint += contribution.value();
            } else if (EffectChannelId.SCREEN_VIGNETTE.equals(channel)) {
                vignette += contribution.value();
            }
        }
        return new EffectContributionSnapshot(speed, fov, roll, shake, tint, vignette);
    }
}
