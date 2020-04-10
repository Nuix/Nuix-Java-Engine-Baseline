Nuix Java Engine Baseline
=========================

![Nuix Engine 8.4](https://img.shields.io/badge/Nuix%20Engine-8.4-green.svg)

View the GitHub project [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline) or download the latest release [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline/releases).

View the Java docs [here](https://nuix.github.io/Nuix-Java-Engine-Baseline/).

# Overview

**Written By:** Jason Wells

This repository contains a series of examples making use of the Java Engine API and can be used as a starting point for your own Nuix Java Engine based project.

# Getting Started

## Setup

- To begin you will need to [download a release](https://download.nuix.com/releases/engine) of the Nuix Java Engine and extract that somewhere on your local machine.  You will want to extract the engine release to a directory with a relatively short name such as the following:
```
D:\engine-releases\8.4.2.466
```
- Have a Nuix license available on a Nuix license dongle, available from a Nuix Management Server instance or available from the Nuix Cloud License Server.
- Download a copy of this repository and open the `Java` sub directory in your IDE of choice.  Add the contents of the `lib` sub directory of your engine release to your project's build path.
- Ensure that the Windows `PATH` environment variable references the `bin` sub directory of your engine release as well as the `bin\x86` directory.  For example if I have my engine distribution located at `D:\engine-releases\8.4.2.466`, I will want to add the following to my `PATH`: `D:\engine-releases\8.4.2.466\bin` and `D:\engine-releases\8.4.2.466\bin\x86`.
- Build your project and export a JAR file.

The package `com.nuix.javaenginebaseline.examples` contains a series of examples demonstrating fundamental activities.  Each example is executable (they have a static void main method).  For example, to run the example [BasicInitializationExample]:

1. Build the project
2. Export a JAR file to a location such as `C:\MyCustomNuixApp\MyApp.jar`
3. Execute the `main` method of the class `BasicInitializationExample` using a 64-bit JRE (Java Runtime Environment) with a command along the lines of:
```
C:\MyCustomNuixApp> java -classpath "D:\engine-releases\8.4.2.466\lib\*;.\*" com.nuix.javaenginesimple.examples.BasicInitializationExample
```

A breakdown of the above command:
- Uses the `-classpath` argument to include on the class path:
	- All jars in `D:\engine-releases\8.4.2.466\lib`
	- All jars in local directory so that `MyApp.jar` is picked up
- Specifies the fully qualified name (`com.nuix.javaenginesimple.examples.BasicInitializationExample`) of our class [BasicInitializationExample] which contains the `public static void main(String[] args)` method (an entry point into the program).

# Basic Overview

In the example above, we start by executing the `main` method of the [BasicInitializationExample] class.  The method begins by creating a new instance of the [EngineWrapper] class, providing it the root directory of the engine release we downloaded earlier.  At this point not much has happened.  Its once we call [withDongleLicense], [withServerLicense] or [withCloudLicense] that the [EngineWrapper] class gets to work.

Note that the classes (such as [EngineWrapper]) provided in this project are not required to use the engine API, instead they demonstrate one way you can implement the Nuix engine initialization process.

## EngineWrapper

The workflow of [withDongleLicense], [withServerLicense] and [withCloudLicense] are fairly similar in the steps they take, differing mostly in how they acquire a license for the engine instance they obtain.  In the following I'll focus on [withDongleLicense] and note some of the differences in behavior to [withServerLicense] and [withCloudLicense].

The method [withDongleLicense] begins by ensuring the specified engine release directory does indeed exist since nothing will work without this.  It then loads the properties in the embedded resource [log4j.properties](https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/Java/src/main/resources/log4j.properties) and uses them to configure [Log4j](https://github.com/apache/log4j).  This file is essentially the same as the one included with a Nuix Workbench install at `<NUIXINSTALL>/config/log4j.properties`.  The method then proceeds to construct a `GlobalContainer` instance (as needed).  There should only ever be 1 `GlobalContainer` instance per Java Virtual Machine.

From the `GlobalContainer` instance an `Engine` instance is constructed, from that a `Licensor` is obtained.  In the case of [withDongleLicense], `Licensor.findAvailableLicences` is called with the setting `sources` having a value of `dongle`.  The method [withServerLicense] instead provides the setting `sources` a value of `server` and [withCloudLicense] provides the value `cloud-server`.

```java
// withDongleLicense
Map<String,Object> licenseOptions = new HashMap<String,Object>();
licenseOptions.put("sources","dongle");

// withServerLicense
Map<String,Object> licenseOptions = new HashMap<String,Object>();
licenseOptions.put("sources","server");

//withCloudLicense
Map<String,Object> licenseOptions = new HashMap<String,Object>();
licenseOptions.put("sources","cloud-server");

Iterable<AvailableLicence> licences = licensor.findAvailableLicences(licenseOptions);
```

`Licensor.findAvailableLicences` returns an `Iterable<AvailableLicence>`.  With this collection we can iterate and inspect each available license, choosing one we wish to acquire and license our Engine instance with.  The [EngineWrapper] class makes use of a [LicenseFilter] object to test each license for some requirements such as having a certain number of workers available, being a certain license type or having particular features.  The default [LicenseFilter] provided by [EngineWrapper] will acquire the first license it finds available, but it can be configured to be more selective.

```java
logger.info("Finding first license which meets filter requirements...");
for(AvailableLicence license : licences) {
	logger.info("\t Count: " + license.getCount());
	logger.info("\t Workers: " + license.getWorkers());
	logger.info("\t Short Name: " + license.getShortName());
	logger.info("\t Type: " + license.getSource().getType());
	logger.info("\t ID: " + license.getSource().getLocation());
	logger.info("\t Description: " + license.getDescription());
	LicenseFeaturesLogger.logFeaturesOfLicense(license);
	
	if(licenseFilter.isValid(license)) {
		if(license.canChooseWorkers()) {
			logger.info(">>>> Acquiring this licence with "+licenseFilter.getMinWorkers()+" workers");
			int targetWorkerCount = licenseFilter.getMinWorkers();
			if(targetWorkerCount < 1) { targetWorkerCount = 2; }
			Map<String,Object> acquireSettings = new HashMap<String,Object>();
			acquireSettings.put("workerCount", targetWorkerCount);
			license.acquire(acquireSettings);
			licenceObtained = true;	
		} else {
			logger.info(">>>> Acquiring this licence");
			license.acquire();
			licenceObtained = true;	
		}
		
		break;
	} else {
		logger.info("<<<< Ignoring this license, does not meet requirements of license filter");
		continue;
	}
}
```

Once a license has been found which is available and meets the requirements of our [LicenseFilter], it is acquired by calling `AvailableLicense.acquire`, causing your Engine instance to capture that license and become licensed until you release the license back.  At this point [withDongleLicense] creates an instance of the Nuix `Utilities` object and provides it to the `accept` method of the `Consumer<Utilities>` object passed to the method.  This is where your code can begin working with the Nuix API, creating cases, processing data, etc.

```java
// EngineWrapper
consumer.accept(utilities);

// Your code
wrapper.withDongleLicense(new Consumer<Utilities>(){
	public void accept(Utilities utilities) {
		// Accept method in Consumer object you provided
		// Called if license was obtained, meaning we should be ready to begin using the Nuix API
	}
});
```

Once your code completes and `Consumer.accept` returns, [withDongleLicense] closes the `Engine` instance by calling `Engine.close` and closes the `GlobalContainer` instance by calling `GlobalContainer.close`, during which the license you acquired is released.

## LicenseFilter

The [LicenseFilter] class is used by [EngineWrapper] while iterating available licenses to determine which license to acquire.  The default instance included with [EngineWrapper] will accept any license, effectively meaning that the first available license is acquired.  You can get the [LicenseFilter] used by an [EngineWrapper] instance by calling [EngineWrapper.getLicenseFilter](https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/EngineWrapper.html#getLicenseFilter--).  Once you have the license filter you can alter it to choose a license based on:
- Whether a license has at least a minimum number of workers available
- Whether a license has no more than a maximum number of workers available
- Whether a license has a particular short name such as `enterprise-workstation`
- Whether a license has one or more [features](https://download.nuix.com/releases/desktop/stable/docs/en/reference/licence-profiles.html)

For example, if I wish to only acquire a license which:
- Has at least 8 workers
- Has the features:
    - `EXPORT_ITEMS`
    - `CASE_CREATION`

You configure the license filter to only acquire licenses meeting this criteria with the following code:

```java
EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\8.4.2.466");

LicenseFilter filter = wrapper.getLicenseFilter();
filter.setMinWorkers(8);
filter.addRequiredFeature("EXPORT_ITEMS");
filter.addRequiredFeature("CASE_CREATION");

try {
	wrapper.withServerLicense("127.0.0.1","username", "password", new Consumer<Utilities>(){
		public void accept(Utilities utilities) {
			// If we have reached here, we should have an instance which has obtained a license
			// with at least 8 workers and features "EXPORT_ITEMS" AND "CASE_CREATION"
		}
	});
} catch (Exception e) {
	logger.error("Unhandled exception",e);
}
```

## LicenseFeaturesLogger

While [EngineWrapper] is iterating each available license, it will also log the feature set available in each license using [LicenseFeatureLogger.logFeaturesOfLicense(license)](https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/LicenseFeaturesLogger.html#logFeaturesOfLicense-nuix.LicenceProperties-).  An example of what this looks like in the logs:

```
2020-04-08 11:35:30 INFO  LicenseFeatures:logFeaturesOfLicense():83 - License Features:
[X] ANALYSIS
[ ] AOS_DATA
[X] AUTOMATIC_CLASSIFIER_EDITING
[ ] AXS_ONE
[X] CASE_CREATION
[X] CYBER_CONTEXT
[X] DESKTOP
[X] ELASTIC_SEARCH
[X] EXCHANGE_WS
[X] EXPORT_CASE_SUBSET
[X] EXPORT_ITEMS
[X] EXPORT_LEGAL
[X] EXPORT_SINGLE_ITEM
[X] EXPORT_VIEW
[X] FAST_REVIEW
[X] GENERAL_DATA
[X] GRAPH
[ ] GWAVA
[X] IMAP_POP
[X] LIGHT_SPEED
[X] LOG_STASH
[X] LOTUS_NOTES
[ ] MAIL_XTENDER
[X] METADATA_IMPORT
[X] MOBILE_DEVICE_IMAGING
[X] NETWORK_DATA
[X] OCR_PROCESSING
[X] OTHER_EMAIL
[X] OUTLOOK
[X] OUTLOOK_EXPRESS
[X] PARTIAL_LOAD
[X] PRODUCTION_SET
[X] SCRIPTING
[ ] SOCIAL_MEDIA
[ ] SYMANTEC_VAULT
[ ] UNRESTRICTED_CASE_ACCESS
[X] WORKER
[X] WORKER_SCRIPTING
[ ] ZANTAZ
```

## ThirdPartyDependencyChecker

Once a license has been obtained by [EngineWrapper] and a `Utilities` object has been obtained from the now licensed `Engine` instance, this class logs third party dependency information via a call to [ThirdPartyDependencyChecker.logAllDependencyInfo(Utilities)](https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/ThirdPartyDependencyChecker.html#logAllDependencyInfo-nuix.Utilities-).  The output of which looks like this:

```
2020-04-08 11:35:45 INFO  ThirdPartyDependencyChecker:logAllDependencyInfo():38 - Reviewing third party dependency statuses:
[X] 'Lotus Notes': Found Version 9.0.1
[ ] 'Microsoft Access': Not found
[X] 'Microsoft Word': Found PDF Capable Word
[X] 'Microsoft Excel': Found PDF Capable Excel
[X] 'Microsoft PowerPoint': Found PDF Capable PowerPoint
[ ] 'Microsoft Visio': Not found
[ ] 'FFmpeg / FFprobe': Not found
[X] 'Relativity': Found
[ ] 'Nuix OCR': Not found
```

## NuixDiagnostics

When there is a problem, it is helpful to capture a snapshot of the state of things for trouble shooting purposes.  In the Nuix Workbench GUI we can generate a diagnostics file with the click of a button.  When using the Java engine API we have to do a bit more work.  The class [NuixDiagnostics] provides a method [saveDiagnostics](https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/NuixDiagnostics.html#saveDiagnostics-java.lang.String-) which can generate a Nuix diagnostics file for you.  The method accepts as an argument the directory (as a String or [java.io.File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)) to which the diagnostics zip will be saved.  The generated zip file will automatically be given a time stamped name in the form `NuixEngineDiagnostics-yyyyMMddHHmmss.zip`, for example `NuixEngineDiagnostics-20200408113545.zip`.

In the example [NuixDiagnostics.saveDiagnostics][saveDiagnostics] is called when an exception bubbles all the way up to our main `try`/`catch`.

```java
public static void main(String[] args) throws Exception {
	EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\8.4.2.466");
	
	try {
		wrapper.withDongleLicense(new Consumer<Utilities>(){
			public void accept(Utilities utilities) {
				// ...
			}
		});
	} catch (Exception e) {
		logger.error("Unhandled exception",e);
		// Lets dump a diagnostics file since something went wrong and having
		// this may be helpful for troubleshooting
		NuixDiagnostics.saveDiagnostics("C:\\EngineDiagnostics");
	}
}
```

# License

```
Copyright 2020 Nuix

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[EngineWrapper]: https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/EngineWrapper.html
[LicenseFilter]: https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/LicenseFilter.html
[withDongleLicense]: https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/EngineWrapper.html#withDongleLicense-java.util.function.Consumer-
[withServerLicense]: https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/EngineWrapper.html#withServerLicense-java.lang.String-java.lang.String-java.lang.String-java.util.function.Consumer-
[withCloudLicense]: https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/Java/src/main/java/com/nuix/javaenginesimple/EngineWrapper.java#L243
[BasicInitializationExample]: https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/Java/src/main/java/com/nuix/javaenginesimple/examples/BasicInitializationExample.java
[NuixDiagnostics]: https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/NuixDiagnostics.html