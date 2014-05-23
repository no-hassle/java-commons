package com.comoyo.emjar;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

public abstract class EmJarTest
{
    protected File getResourceFile(String name) {
        final ClassLoader cl = getClass().getClassLoader();
        final URL url = cl.getResource("com/comoyo/emjar/" + name);
        if (!"file".equals(url.getProtocol())) {
            throw new IllegalArgumentException(
                "Resource " + name + " not present as file (" + url + ")");
        }
        return new File(URLDecoder.decode(url.getPath()));
    }
}
