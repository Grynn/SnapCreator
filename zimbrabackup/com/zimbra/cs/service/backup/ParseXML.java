// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ParseXML.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

public class ParseXML
{

    public ParseXML()
    {
    }

    public static FileCopierOptions parseFileCopierOptions(Element fcOptsElem)
        throws ServiceException
    {
        FileCopierOptions opts = new FileCopierOptions();
        String str = fcOptsElem.getAttribute("fcMethod");
        com.zimbra.common.io.FileCopierOptions.Method method = com.zimbra.common.io.FileCopierOptions.Method.parseMethod(str);
        opts.setMethod(method);
        com.zimbra.common.io.FileCopierOptions.IOType ioType = com.zimbra.common.io.FileCopierOptions.IOType.OIO;
        str = fcOptsElem.getAttribute("fcIOType", null);
        if(str != null)
        {
            ioType = com.zimbra.common.io.FileCopierOptions.IOType.parseIOType(str);
            opts.setIOType(ioType);
        }
        if(ioType.equals(com.zimbra.common.io.FileCopierOptions.IOType.OIO))
        {
            int num = (int)fcOptsElem.getAttributeLong("fcOIOCopyBufferSize", -1L);
            if(num != -1)
                opts.setOIOCopyBufferSize(num);
        }
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

        switch(_cls1..SwitchMap.com.zimbra.common.io.FileCopierOptions.Method[method.ordinal()])
        {
        default:
            break;

        case 1: // '\001'
        {
            int num = (int)fcOptsElem.getAttributeLong("fcAsyncQueueCapacity", -1L);
            if(num != -1)
                opts.setAsyncQueueCapacity(num);
            num = (int)fcOptsElem.getAttributeLong("fcParallelWorkers", -1L);
            if(num != -1)
                opts.setNumParallelWorkers(num);
            break;
        }

        case 2: // '\002'
        {
            int num = (int)fcOptsElem.getAttributeLong("fcAsyncQueueCapacity", -1L);
            if(num != -1)
                opts.setAsyncQueueCapacity(num);
            num = (int)fcOptsElem.getAttributeLong("fcPipes", -1L);
            if(num != -1)
                opts.setNumPipes(num);
            num = (int)fcOptsElem.getAttributeLong("fcPipeBufferSize", -1L);
            if(num != -1)
                opts.setPipeBufferSize(num);
            num = (int)fcOptsElem.getAttributeLong("fcPipeReadersPerPipe", -1L);
            if(num != -1)
                opts.setNumReadersPerPipe(num);
            num = (int)fcOptsElem.getAttributeLong("fcPipeWritersPerPipe", -1L);
            if(num != -1)
                opts.setNumWritersPerPipe(num);
            break;
        }
        }
        return opts;
    }
}
