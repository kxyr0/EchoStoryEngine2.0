package io.github.kxyr0.echoplayerfollow.server;

import java.util.Optional;

record FollowSearchResult(Optional<FollowCandidate> candidate, String reason) {
    static FollowSearchResult found(FollowCandidate candidate) {
        return new FollowSearchResult(Optional.of(candidate), "");
    }

    static FollowSearchResult rejected(String reason) {
        return new FollowSearchResult(Optional.empty(), reason);
    }
}
