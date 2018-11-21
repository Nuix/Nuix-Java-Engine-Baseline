package com.nuix.javaenginesimple;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class NuixDiagnostics {
	// Obtain a logger instance for this class
	private final static Logger logger = Logger.getLogger(NuixDiagnostics.class);
		
	public static void saveDiagnostics(File directory){
		List<MBeanServer> beanServers = new ArrayList<MBeanServer>();
		beanServers.add(ManagementFactory.getPlatformMBeanServer());
		beanServers.addAll(MBeanServerFactory.findMBeanServer(null));
		for (MBeanServer mBeanServer : beanServers) {
			Set<ObjectName> objectNames = mBeanServer.queryNames(null, null);
			for (ObjectName beanName : objectNames) {
				if(beanName.toString().contains("DiagnosticsControl")){
					directory.mkdirs();
					DateTime timeStamp = DateTime.now();
					String timeStampString = timeStamp.toString("YYYYMMDDHHmmss");
					File zipFile = new File(directory,"NuixEngineDiagnostics-"+timeStampString+".zip");
					try {
						mBeanServer.invoke(beanName,"generateDiagnostics",new Object[] {zipFile.getPath()},new String[] {"java.lang.String"});
						return;
					} catch (Exception e) {
						logger.error("Error saving diagnostics", e);
					}
				}
			}
		}
	}
	
	public static void saveDiagnostics(String directory) { saveDiagnostics(new File(directory)); }
}
