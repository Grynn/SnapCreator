// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ToXML.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

public class ToXML
{

    public ToXML()
    {
    }

    public static void encodeFileCopierOptions(Element parent, FileCopierOptions fcOpts)
        throws ServiceException
    {
        Element elem = parent.addElement("fileCopier");
        elem.addAttribute("fcMethod", fcOpts.getMethod().toString());
        elem.addAttribute("fcIOType", fcOpts.getIOType().toString());
        if(fcOpts.getIOType().equals(com.zimbra.common.io.FileCopierOptions.IOType.OIO))
            elem.addAttribute("fcOIOCopyBufferSize", fcOpts.getOIOCopyBufferSize());
        static class _cls1
        {

            static final int $SwitchMap$com$zimbra$common$io$FileCopierOptions$Method[];

            static 
            {
                $SwitchMap$com$zimbra$common$io$FileCopierOptions$Method = new int[com.zimbra.common.io.FileCopierOptions.Method.values().length];
                try
                {
                    $SwitchMap$com$zimbra$common$io$FileCopierOptions$Method[com.zimbra.common.io.FileCopierOptions.Method.PARALLEL.ordinal()] = 1;
                }
                catch(NoSuchFieldError ex) { }
                try
                {
                    $SwitchMap$com$zimbra$common$io$FileCopierOptions$Method[com.zimbra.common.io.FileCopierOptions.Method.PIPE.ordinal()] = 2;
                }
                catch(NoSuchFieldError ex) { }
            }
        }

        switch(_cls1..SwitchMap.com.zimbra.common.io.FileCopierOptions.Method[fcOpts.getMethod().ordinal()])
        {
        case 1: // '\001'
            elem.addAttribute("fcAsyncQueueCapacity", fcOpts.getAsyncQueueCapacity());
            elem.addAttribute("fcParallelWorkers", fcOpts.getNumParallelWorkers());
            break;

        case 2: // '\002'
            elem.addAttribute("fcAsyncQueueCapacity", fcOpts.getAsyncQueueCapacity());
            elem.addAttribute("fcPipes", fcOpts.getNumPipes());
            elem.addAttribute("fcPipeBufferSize", fcOpts.getPipeBufferSize());
            elem.addAttribute("fcPipeReadersPerPipe", fcOpts.getNumReadersPerPipe());
            elem.addAttribute("fcPipeWritersPerPipe", fcOpts.getNumWritersPerPipe());
            break;
        }
    }
}
