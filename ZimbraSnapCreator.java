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
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.BasicConfigurator;

public class ZimbraSnapCreator {
	static Logger logger = Logger.getLogger(ZimbraSnapCreator.class.getName());

	public static void main(String[] args) {
		BasicConfigurator.configure();
		logger.info("Entering application");
		logger.info(args[0]);
		logger.info(args[1]);
		if (args.length > 2) { logger.info(args[2]); }
	
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		if (args[0].contains("account=")) {
			if (args[1].contains("status=")) {
				String[] status = args[1].split("=");
				String[] account = args[0].split("=");
				String serverName = args[2];

				setAccountStatus(account[1], status[1], serverName);
			} else {
				String[] account = args[0].split("=");
				String serverName = args[1];
				
				getAccountStatus(account[1], serverName);
			}		
		} else {
			bulkUpdate(args[0], args[1], Integer.parseInt(args[2]));
		}
		logger.info("Exiting application.");
	}

	private static void setAccountStatus(String account, String status, String serverName) {
		ZimbraProvider provider;

		provider = new ZimbraProvider(new ThreadParam(0, 0, 0, status, serverName));		
		provider.setAccountStatus(account,status);	

	}

	private static void getAccountStatus(String account, String serverName) {
		ZimbraProvider provider;
		
		provider = new ZimbraProvider(new ThreadParam(0, 0, 0, "", serverName));
		System.out.println(provider.getAccountStatus(account));
	}

	private static void bulkUpdate(String status, String serverName, int threadCount) {
        
		Provisioning prov = Provisioning.getInstance();
		List<Thread> threads = new ArrayList<Thread>(); 
		List accts;	
		int range=0;
		int remainder=0;
System.out.println("Bulk update");
		try {	
			accts = prov.searchAccounts((new StringBuilder()).append("(zimbraMailHost=").append(serverName).append(")").toString(), new String[] {"zimbraId"}, null, false, 521);
/*	
	Server server = prov.getLocalServer();
	serverName = server.getAttr("zimbraServiceHostname");
      
          SearchAccountsOptions searchOpts = new SearchAccountsOptions(new String[] {
              "zimbraId"
          });
          searchOpts.setMakeObjectOpt(com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt.NO_SECONDARY_DEFAULTS);
          searchOpts.setSortOpt(com.zimbra.cs.account.SearchDirectoryOptions.SortOpt.SORT_DESCENDING);
          accts = prov.searchAccountsOnServer(server, searchOpts);
          Account a[] = (Account[])(Account[])accts.toArray(new Account[0]);	
*/
	range = accts.size() / threadCount;
	remainder = accts.size() % threadCount;
	logger.info(String.format("number of accounts: %d", accts.size()));
	logger.info(String.format("range: %d", range));
} catch(ServiceException ex) {
	logger.warn(ex.getMessage());
	System.out.println(ex.getMessage());
}
	int min=0;
	int max = range;
	ZimbraProvider provider;
	List<ZimbraProvider> providers = new ArrayList<ZimbraProvider>();
	for(int i=0; i < threadCount-1; i++) {
		provider = new ZimbraProvider(new ThreadParam(i, min, max,status, serverName));		
		providers.add(provider);
		min += range;
		max += range;
	}	
	provider = new ZimbraProvider(new ThreadParam(threadCount-1,min,max+remainder,status, serverName));
	providers.add(provider);

	int count=0;
	for(ZimbraProvider p : providers) {
		Thread t = new Thread(p);
		threads.add(t);
		logger.info(String.format("Adding thread: %d", count));
		count++;
	}
	for(Thread t : threads) {
		t.start();
	}
		
	try {
		for(Thread t: threads) {
			t.join();
		}	
	} catch (InterruptedException ex) {
		ex.printStackTrace();
	}

 	}
}
