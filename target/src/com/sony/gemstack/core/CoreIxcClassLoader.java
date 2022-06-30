package com.sony.gemstack.core;
    
public interface CoreIxcClassLoader {
    Class findLoadedProxyClass(String str);
    Class defineProxyClass(String str, byte[] bytes);
}
