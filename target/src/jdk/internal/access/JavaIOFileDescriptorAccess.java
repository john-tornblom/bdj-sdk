package jdk.internal.access;

import java.io.FileDescriptor;

public interface JavaIOFileDescriptorAccess {
    int get(FileDescriptor fd);
}
