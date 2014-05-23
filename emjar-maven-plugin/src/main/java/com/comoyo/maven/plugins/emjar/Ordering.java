package com.comoyo.maven.plugins.emjar;

public class Ordering
{
    private String prefer;
    private String over;

    public Ordering() {}

    public Ordering(String prefer, String over)
    {
        this.prefer = prefer;
        this.over = over;
    }

    public String getPrefer()
    {
        return prefer;
    }

    public String getOver()
    {
        return over;
    }
}
