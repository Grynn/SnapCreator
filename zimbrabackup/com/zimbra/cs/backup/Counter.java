// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Counter.java

package com.zimbra.cs.backup;


public class Counter
{

    public Counter(String name, String unit, long sum, long numSamples)
    {
        mName = name;
        mUnit = unit;
        mSum = sum;
        mNumSamples = numSamples;
    }

    public Counter(String name, String unit)
    {
        this(name, unit, 0L, 0L);
    }

    public String getName()
    {
        return mName;
    }

    public String getUnit()
    {
        return mUnit;
    }

    public long getSum()
    {
        return mSum;
    }

    public long getNumSamples()
    {
        return mNumSamples;
    }

    public double getAverage()
    {
        if(mSum == 0L)
        {
            return 0.0D;
        } else
        {
            double sum = mSum;
            double numSamples = mNumSamples;
            return sum / numSamples;
        }
    }

    public void add(long value)
    {
        mSum += value;
        mNumSamples++;
    }

    public void increment()
    {
        add(1L);
    }

    public void reset()
    {
        mSum = 0L;
        mNumSamples = 0L;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder(mName);
        sb.append(": ").append(mSum);
        if(mUnit != null)
            sb.append(" ").append(mUnit);
        sb.append("(").append(mNumSamples);
        sb.append(" @ ").append(String.format("%.2f", new Object[] {
            Double.valueOf(getAverage())
        }));
        if(mUnit != null)
            sb.append(" ").append(mUnit);
        sb.append(")");
        return sb.toString();
    }

    private String mName;
    private String mUnit;
    private long mSum;
    private long mNumSamples;
}
