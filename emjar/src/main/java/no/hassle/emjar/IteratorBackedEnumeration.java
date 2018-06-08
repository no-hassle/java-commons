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

package no.hassle.emjar;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorBackedEnumeration<E>
    implements Enumeration<E>
{
    private final Iterator<E> it;

    public IteratorBackedEnumeration(Iterator<E> it)
    {
        this.it = it;
    }

    public boolean hasMoreElements()
    {
        return it.hasNext();
    }

    public E nextElement()
    {
        return it.next();
    }
}
