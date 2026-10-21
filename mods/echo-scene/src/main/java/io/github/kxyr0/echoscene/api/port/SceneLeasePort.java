package io.github.kxyr0.echoscene.api.port;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;

public interface SceneLeasePort {
    LeaseResult acquire(SceneExecutionId owner, String rootOwner, SceneCapability capability);

    void releaseOwned(SceneExecutionId owner);

    record LeaseResult(boolean acquired, String detail) {
        public static LeaseResult granted() {
            return new LeaseResult(true, "");
        }

        public static LeaseResult refused(String detail) {
            return new LeaseResult(false, detail);
        }
    }
}
