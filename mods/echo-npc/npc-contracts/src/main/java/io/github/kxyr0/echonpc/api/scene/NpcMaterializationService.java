package io.github.kxyr0.echonpc.api.scene;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;

/**
 * Authored scene placement surface. SpawnAt never invents entity identity.
 */
public interface NpcMaterializationService {
    NpcResults.Spawn spawnAt(SpawnNpcRequest request);

    boolean isMaterialized(ActorId npcId);

    NpcSceneReadinessView sceneReadiness(ActorId npcId);
}
