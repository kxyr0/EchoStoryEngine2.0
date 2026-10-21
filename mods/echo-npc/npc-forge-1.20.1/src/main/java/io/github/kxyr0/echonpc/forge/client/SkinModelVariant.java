package io.github.kxyr0.echonpc.forge.client;

enum SkinModelVariant {
    CLASSIC("CLASSIC"),
    SLIM("SLIM");

    private final String apiName;

    SkinModelVariant(String apiName) {
        this.apiName = apiName;
    }

    String apiName() {
        return apiName;
    }
}
