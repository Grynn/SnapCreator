// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   TestCreateMessage.java

package com.zimbra.qa.unittest;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.backup.BackupRestoreFileCopierOptionsCLI;
import com.zimbra.cs.backup.BackupUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.store.*;
import com.zimbra.cs.store.file.Volume;
import java.io.IOException;
import java.util.*;
import javax.mail.MessagingException;
import junit.framework.TestCase;

// Referenced classes of package com.zimbra.qa.unittest:
//            MessageBuilder, TestUtil

public class TestCreateMessage extends TestCase
{
    private class MessageProperties
    {

        String subject;
        String messageContent;
        boolean isBlobCompressed;
        byte blobData[];
        final TestCreateMessage this$0;

        MessageProperties(String subject, String messageContent, boolean isBlobCompressed)
            throws IOException
        {
            this$0 = TestCreateMessage.this;
            super();
            this.subject = subject;
            this.messageContent = messageContent;
            this.isBlobCompressed = isBlobCompressed;
            if(isBlobCompressed)
                blobData = ByteUtil.compress(messageContent.getBytes());
            else
                blobData = messageContent.getBytes();
        }
    }


    public TestCreateMessage()
    {
    }

    public void setUp()
        throws Exception
    {
        SENDER_ADDRESS = TestUtil.getAddress("user1");
        RECIPIENT1_ADDRESS = TestUtil.getAddress("user2");
        RECIPIENT2_ADDRESS = TestUtil.getAddress("user3");
        cleanUp();
        Volume v = Volume.getCurrentMessageVolume();
        mOriginalCompressBlobs = v.getCompressBlobs();
        mOriginalCompressionThreshold = v.getCompressionThreshold();
        mOriginalDiskStreamingThreshold = TestUtil.getServerAttr("zimbraMailDiskStreamingThreshold");
    }

    public void testCreateMessage()
        throws Exception
    {
        String subject = "TestCreateMessage SS";
        MessageProperties messageSS = new MessageProperties(subject, getMessageContent(subject, 10, 0), false);
        subject = "TestCreateMessage MS";
        MessageProperties messageMS = new MessageProperties(subject, getMessageContent(subject, 1000, 0), true);
        subject = "TestCreateMessage MM";
        MessageProperties messageMM = new MessageProperties(subject, getMessageContent(subject, 1000, 100), true);
        subject = "TestCreateMessage LS";
        MessageProperties messageLS = new MessageProperties(subject, getMessageContent(subject, 10000, 0), true);
        subject = "TestCreateMessage LM";
        MessageProperties messageLM = new MessageProperties(subject, getMessageContent(subject, 10000, 50), true);
        subject = "TestCreateMessage LL";
        MessageProperties messageLL = new MessageProperties(subject, getMessageContent(subject, 10000, 100), true);
        List allMessages = new ArrayList();
        allMessages.add(messageSS);
        allMessages.add(messageMS);
        allMessages.add(messageMM);
        allMessages.add(messageLS);
        allMessages.add(messageLM);
        allMessages.add(messageLL);
        int newCompressionThreshold = Math.min(messageMS.messageContent.length(), messageMM.messageContent.length()) - 1;
        int newDiskStreamingThreshold = Math.min(messageLS.messageContent.length(), Math.min(messageLM.messageContent.length(), messageLL.messageContent.length())) - 1;
        Volume v = Volume.getCurrentMessageVolume();
        Volume.update(v.getId(), v.getType(), v.getName(), v.getRootPath(), v.getMboxBits(), v.getMboxGroupBits(), v.getFileGroupBits(), v.getFileBits(), true, newCompressionThreshold);
        TestUtil.setServerAttr("zimbraMailDiskStreamingThreshold", Integer.toString(newDiskStreamingThreshold));
        SoapTransport transport = TestUtil.getAdminSoapTransport();
        BackupUtil.fullBackup(new String[] {
            RECIPIENT1_ADDRESS, RECIPIENT2_ADDRESS
        }, true, false, false, new FileCopierOptions(), null, null, transport);
        String recipients[] = {
            RECIPIENT1_ADDRESS
        };
        MessageProperties mp;
        for(Iterator i$ = allMessages.iterator(); i$.hasNext(); TestUtil.addMessageLmtp(recipients, SENDER_ADDRESS, mp.messageContent))
            mp = (MessageProperties)i$.next();

        recipients = (new String[] {
            RECIPIENT1_ADDRESS, RECIPIENT2_ADDRESS
        });
        MessageProperties mp;
        for(Iterator i$ = allMessages.iterator(); i$.hasNext(); TestUtil.addMessageLmtp(recipients, SENDER_ADDRESS, mp.messageContent))
            mp = (MessageProperties)i$.next();

        Mailbox recip1Mbox = TestUtil.getMailbox("user2");
        Mailbox recip2Mbox = TestUtil.getMailbox("user3");
        MessageProperties mp;
        List ids;
        for(Iterator i$ = allMessages.iterator(); i$.hasNext(); verifyBlobContent(recip2Mbox, mp, ((Integer)ids.get(0)).intValue()))
        {
            mp = (MessageProperties)i$.next();
            ids = TestUtil.search(recip1Mbox, (new StringBuilder()).append("in:inbox subject:\"").append(mp.subject).append("\"").toString(), (byte)5);
            assertEquals(2, ids.size());
            verifyBlobContent(recip1Mbox, mp, ((Integer)ids.get(0)).intValue());
            verifyBlobContent(recip1Mbox, mp, ((Integer)ids.get(1)).intValue());
            ids = TestUtil.search(recip2Mbox, (new StringBuilder()).append("in:inbox subject:\"").append(mp.subject).append("\"").toString(), (byte)5);
        }

        com.zimbra.common.soap.Element.XMLElement req = new com.zimbra.common.soap.Element.XMLElement(BackupService.RESTORE_REQUEST);
        Element restore = req.addElement("restore");
        restore.addAttribute("method", "ca");
        restore.addAttribute("prefix", "TestCreateMessage-");
        restore.addElement(new com.zimbra.common.soap.Element.XMLElement("account")).addAttribute("name", RECIPIENT1_ADDRESS);
        restore.addElement(new com.zimbra.common.soap.Element.XMLElement("account")).addAttribute("name", RECIPIENT2_ADDRESS);
        Element res = TestUtil.getAdminSoapTransport().invoke(req);
        assertEquals("ok", res.getAttribute("status"));
        recip1Mbox = TestUtil.getMailbox("TestCreateMessage-user2");
        recip2Mbox = TestUtil.getMailbox("TestCreateMessage-user3");
        MessageProperties mp;
        List ids;
        for(Iterator i$ = allMessages.iterator(); i$.hasNext(); verifyBlobContent(recip2Mbox, mp, ((Integer)ids.get(0)).intValue()))
        {
            mp = (MessageProperties)i$.next();
            String context = String.format("Message '%s'", new Object[] {
                mp.subject
            });
            ids = TestUtil.search(recip1Mbox, (new StringBuilder()).append("in:inbox subject:\"").append(mp.subject).append("\"").toString(), (byte)5);
            assertEquals(context, 2, ids.size());
            verifyBlobContent(recip1Mbox, mp, ((Integer)ids.get(0)).intValue());
            verifyBlobContent(recip1Mbox, mp, ((Integer)ids.get(1)).intValue());
            ids = TestUtil.search(recip2Mbox, (new StringBuilder()).append("in:inbox subject:\"").append(mp.subject).append("\"").toString(), (byte)5);
        }

    }

    private void verifyBlobContent(Mailbox mbox, MessageProperties mp, int messageId)
        throws Exception
    {
        Message message = mbox.getMessageById(null, messageId);
        MailboxBlob mblob = message.getBlob();
        java.io.File file = mblob.getLocalBlob().getFile();
        byte blobData[] = ByteUtil.getContent(file);
        java.io.InputStream in = StoreManager.getInstance().getContent(mblob);
        byte uncompressedData[] = ByteUtil.getContent(in, mp.messageContent.length());
        String context = String.format("Message '%s', stored at %s.", new Object[] {
            mp.subject, mblob.getLocalBlob().getPath()
        });
        assertEquals(context, mp.isBlobCompressed, mblob.getLocalBlob().isCompressed());
        assertEquals(context, mp.isBlobCompressed, ByteUtil.isGzipped(blobData));
        TestUtil.assertMessageContains(new String(uncompressedData), mp.messageContent);
    }

    private static String getMessageContent(String subject, int numWords, int percentRandomWords)
        throws ServiceException, IOException, MessagingException
    {
        StringBuilder buf = new StringBuilder();
        for(int i = 1; i <= numWords; i++)
        {
            boolean isRandom = RANDOM.nextInt(100) < percentRandomWords;
            if(isRandom)
                buf.append(getRandomWord(7));
            else
                buf.append("word");
            if(i % 10 == 0)
                buf.append("\r\n");
            else
                buf.append(" ");
        }

        return (new MessageBuilder()).withSubject(subject).withBody(buf.toString()).withToRecipient("user2").withFrom("user1").create();
    }

    private static String getRandomWord(int maxLength)
    {
        int length = RANDOM.nextInt(maxLength) + 1;
        char data[] = new char[length];
        for(int i = 0; i < length; i++)
            data[i] = (char)(97 + RANDOM.nextInt(26));

        return new String(data);
    }

    public void tearDown()
        throws Exception
    {
        cleanUp();
        Volume v = Volume.getCurrentMessageVolume();
        Volume.update(v.getId(), v.getType(), v.getName(), v.getRootPath(), v.getMboxBits(), v.getMboxGroupBits(), v.getFileGroupBits(), v.getFileBits(), mOriginalCompressBlobs, mOriginalCompressionThreshold);
        TestUtil.setServerAttr("zimbraMailDiskStreamingThreshold", mOriginalDiskStreamingThreshold);
    }

    private void cleanUp()
        throws Exception
    {
        TestUtil.deleteTestData("user2", "TestCreateMessage");
        TestUtil.deleteTestData("user3", "TestCreateMessage");
        BackupUtil.deleteBackups("0d", null, TestUtil.getAdminSoapTransport());
        TestUtil.deleteAccount("TestCreateMessage-user2");
        TestUtil.deleteAccount("TestCreateMessage-user3");
    }

    private static final String NAME_PREFIX = "TestCreateMessage";
    private static final Random RANDOM = new Random();
    private static final String SENDER_NAME = "user1";
    private static String SENDER_ADDRESS;
    private static final String RECIPIENT1_NAME = "user2";
    private static String RECIPIENT1_ADDRESS;
    private static final String RECIPIENT2_NAME = "user3";
    private static String RECIPIENT2_ADDRESS;
    private static final String RESTORE_ACCOUNT_PREFIX = "TestCreateMessage-";
    private static final String RESTORE_ACCOUNT1_NAME = "TestCreateMessage-user2";
    private static final String RESTORE_ACCOUNT2_NAME = "TestCreateMessage-user3";
    private String mOriginalDiskStreamingThreshold;
    private long mOriginalCompressionThreshold;
    private boolean mOriginalCompressBlobs;

}
