import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.*;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.service.backup.ToXML;
import com.zimbra.cs.util.BuildInfo;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.commons.cli.*;
 import com.zimbra.cs.account.Provisioning;
 import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Account;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ZimbraProvider implements Runnable {
                static Logger logger = Logger.getLogger(ZimbraProvider.class.getName());
		private ThreadParam threadParam; 
                public ZimbraProvider(Object parameter) {
                        threadParam = (ThreadParam)parameter;
                }

                public void run() {
                        Provisioning prov = Provisioning.getInstance();
			//System.out.println("Thread " + threadParam.getThreadId() + " starting.");
			try {
                        List accts = prov.searchAccounts((new StringBuilder()).append("(zimbraMailHost=").append(threadParam.getZimbraHost()).append(")").toString(), new String[] {"zimbraId"}, null, false, 521);
                        //System.out.println("Thread " + threadParam.getThreadId() + " from " + threadParam.getMin() + " to " + threadParam.getMax());
			for(int i=threadParam.getMin(); i < threadParam.getMax(); i++) {
                                NamedEntry account = (NamedEntry)accts.get(i);
                              //  System.out.println(threadParam.getThreadId() + ": " + account.getName());
                                Account a = prov.getAccount(account.getName());
				logger.info(String.format("thread: %d account: %s set to: %s", threadParam.getThreadId(), account.getName(), threadParam.getStatus()));
                                Map<String, Object> attrs = new HashMap<String, Object>();
                                attrs.put("zimbraAccountStatus", threadParam.getStatus());
                                prov.modifyAttrs(a, attrs, true);
                        } 
			} catch (ServiceException e ) 
		{
			e.printStackTrace();
		}
                }

       		public void setAccountStatus(String account, String status) {
			Provisioning prov = Provisioning.getInstance();

			try {
				Account a = prov.getAccount(account);
				Map<String, Object> attrs = new HashMap<String, Object>();
				attrs.put("zimbraAccountStatus", status);
				prov.modifyAttrs(a, attrs, true);
			} catch (ServiceException e ) {
				logger.info(String.format("thread: %d setAccountStatus: %s to %s : %s", threadParam.getThreadId(), account, status, e.getMessage()));
			}

		}

		public String getAccountStatus(String account) {
			Provisioning prov = Provisioning.getInstance();

			try {
				Account a = prov.getAccount(account);
				return a.getAccountStatusAsString(); 
			} catch (ServiceException e ) {
				logger.info(String.format("thread: %d getAccountStatus: %s : %s", threadParam.getThreadId(), account, e.getMessage()));
			}
			return "";
		}
        }
