package com.nuix.enginebaseline;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTime;

public class NuixDiagnostics {
	private final static Logger logger = LogManager.getLogger(NuixDiagnostics.class);
		
	public static void saveDiagnosticsToFile(File zipFile){
		List<MBeanServer> beanServers = new ArrayList<MBeanServer>();
		beanServers.add(ManagementFactory.getPlatformMBeanServer());
		beanServers.addAll(MBeanServerFactory.findMBeanServer(null));
		for (MBeanServer mBeanServer : beanServers) {
			Set<ObjectName> objectNames = mBeanServer.queryNames(null, null);
			for (ObjectName beanName : objectNames) {
				if(beanName.toString().contains("DiagnosticsControl")){
					zipFile.mkdirs();
					
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
	
	public static void saveDiagnosticsToFile(String zipFile) { saveDiagnosticsToFile(new File(zipFile)); }
	
	public static void saveDiagnosticsToDirectory(File directory) {
		DateTime timeStamp = DateTime.now();
		String timeStampString = timeStamp.toString("yyyyMMddHHmmss");
		File zipFile = new File(directory,"NuixEngineDiagnostics-"+timeStampString+".zip");
		saveDiagnosticsToFile(zipFile);
	}
	
	public static void saveDiagnosticsToDirectory(String directory) {
		saveDiagnosticsToDirectory(new File(directory));
	}
}
