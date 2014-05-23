package com.comoyo.emjar;

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
