package io.github.kxyr0.echocontentdoctor.provider;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import java.util.List;

public interface ContentSnapshotProvider {
    long revision();

    int files();

    List<ContentDescriptor> descriptors();
}
