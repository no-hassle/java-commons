/**
 * Copyright (C) 2014 Telenor Digital AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
