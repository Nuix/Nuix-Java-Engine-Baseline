Nuix Java Engine Baseline
=========================

![Nuix Engine 7.6](https://img.shields.io/badge/Nuix%20Engine-7.6-green.svg)

View the GitHub project [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline) or download the latest release [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline/releases).

View the Java docs [here](https://nuix.github.io/Nuix-Java-Engine-Baseline/).

# Overview

**Written By:** Jason Wells

This is a base line example of a Java project making use of the Java Engine API and can be used as a starting point for your own Nuix Java Engine based project.

# Getting Started

## Setup

- To begin you will need to [download](https://download.nuix.com/releases/engine) a release of the Nuix Java Engine and extract that somewhere on your local machine.  You will want to extract the engine release to a directory with a relatively short name such as `D:\engine-releases\7.6.5`.  The short directory name is due to the way the class path is passed to Nuix Worker processes, a longer directory name can cause issues later on.
- Have a Nuix license available, be it a license dongle or a Nuix Management Server instance you can obtain a license from.
- Download a copy of this repository and open the `Java` sub directory in your IDE of choice.  Add the contents of the `lib` sub directory of your engine release to your build path.
- Ensure that the Windows `PATH` environment variable references the `bin` sub directory of your engine release as well as the 'bin\x86' directory.  For example if I have my engine distribution located at `D:\engine-releases\7.6.6`, I will want to add the following to my `PATH`: `D:\engine-releases\7.6.6\bin` and `D:\engine-releases\7.6.6\bin\x86`.
- Build the project, executing the `main` method of the class `EntryPoint`.

# Code Overview

In the example we start at the `main` method of the `EntryPoint` class.  The method begins by creating a new instance of the `EngineWrapper` class, providing it the root directory of the engine release we downloaded earlier.  At this point not much has happened.  Its once we call `withDongleLicense` or `withServerLicense` that the `EngineWrapper` class gets to work.

## EngineWrapper

The workflow of `withDongleLicense` and `withServerLicense` are basically the same with the exception that one instructs Nuix to look for a dongle and the other tell Nuix to connect to a Nuix Management Server with a particular user name and password.  In the following I'll focus on `withDongleLicense` and note differences in behavior by `withServerLicense`.

The method begins by ensuring the specified engine release directory does indeed exist since nothing will work without this.  The method then loads the properties in the embedded resource `log4j.properties`.  This file contains various properties related to how [Log4j](https://github.com/apache/log4j) should be configured.  This file is essentially the same as the one included with a Nuix Workbench install at `/config/log4j.properties`.  The method then proceeds to construct a `GlobalContainer` instance, of which there should only ever be 1 per Java Virtual Machine.

From the `GlobalContainer` instance an `Engine` instance is constructed, from that a `Licensor` is obtained.  In the case of `withDongleLicense`, `Licensor.findAvailableLicences` is called with the setting `sources` having a value of `dongle`.  `withServerLicence` instead provides the setting `sources` a value of `server`.

```java
// withDongleLicense
Map<String,Object> licenseOptions = new HashMap<String,Object>();
licenseOptions.put("sources","dongle");

// withServerLicense
Map<String,Object> licenseOptions = new HashMap<String,Object>();
licenseOptions.put("sources","server");

Iterable<AvailableLicence> licences = licensor.findAvailableLicences(licenseOptions);
```

`Licensor.findAvailableLicences` returns an `Iterable<AvailableLicence>`.  This is where we can begin to inspect the licenses available.  The `EngineWrapper` class makes use of a `LicenseFilter` object to test each license for some requirements such as having a certain number of workers available, certain license type or particular features.  The `EngineWrapper` defaults to an instance of `LicenseFilter` which will approve any license that is available.

```java
// Iterate available licenses
for(AvailableLicence license : licences) {
	logger.info("\t Count: " + license.getCount());
	logger.info("\t Workers: " + license.getWorkers());
	logger.info("\t Short Name: " + license.getShortName());
	logger.info("\t Type: " + license.getSource().getType());
	logger.info("\t ID: " + license.getSource().getLocation());
	logger.info("\t Description: " + license.getDescription());
	LicenseFeaturesLogger.logFeaturesOfLicense(license);
	
	if(licenseFilter.isValid(license)) {
		logger.info(">>>> Acquiring this licence");
		license.acquire();
		licenceObtained = true;
	} else {
		logger.info("<<<< Ignoring this license, does not meet requirements of license filter");
		continue;
	}
	
	break;
}
```

Once a license has been found which is available and passes requirements of the license filter, its is acquired by calling `AvailableLicense.acquire`, making your engine instance licensed and that license exclusive to your application.  At this point `withDongleLicense` creates and instance of the Nuix `Utilities` object and provides it to the `accept` method of the `Consumer<Utilities>` object passed to the method.  This is where your code can begin working with the Nuix API, creating cases, processing data, etc.

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

Once your code completes and `Consumer.accept` returns, `withDongleLicense` closes the `Engine` instance and `GlobalContainer` instance, during which the license you acquired is released.

## LicenseFilter

This class is used by `EngineWrapper` while iterating available licenses to determine which license to acquire.  The default instance included with `EngineWrapper` will accept any license, effectively meaning that the first available license is acquired.  You can get the `LicenseFilter` used by an `EngineWrapper` instance by calling `EngineWrapper.getLicenseFilter`.  Once you have the license filter you can alter it to choose a license based on:
- Whether a license has at least a minimum number of workers available
- Whether a license has no more than a maximum number of workers available
- Whether a license has a particular short name such as `enterprise-workstation`
- Whether a license has one or more [features](https://download.nuix.com/releases/desktop/stable/docs/en/reference/licence-profiles.html)

For example, if I wish to only acquire a license which
- Has at least 8 workers
- Has the features:
    - `EXPORT_ITEMS`
    - `CASE_CREATION`

I can do the following:

```java
EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\7.6.5");

LicenseFilter filter = wrapper.getLicenseFilter();
filter.setMinWorkers(8);
filter.addRequiredFeature("EXPORT_ITEMS");
filter.addRequiredFeature("CASE_CREATION");

try {
	wrapper.withServerLicense("username", "password", new Consumer<Utilities>(){
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

While `EngineWrapper` is iterating each available license, it will also log the feature set available in each license using `LicenseFeatureLogger.logFeaturesOfLicense(license)`.  An example of what this looks like in the logs:

```
2018-11-28 11:35:30 INFO  LicenseFeatures:logFeaturesOfLicense():83 - License Features:
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

Once a license has been obtained by `EngineWrapper` and a `Utilities` object has been obtained from the now licensed `Engine` instance, this class logs third party dependency information via a call to `ThirdPartyDependencyChecker.logAllDependencyInfo(Utilities)`.  The output of which looks like this:

```
2018-11-28 11:35:30 INFO  ThirdPartyDependencyChecker:logAllDependencyInfo():38 - Reviewing third party dependency statuses:
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

When there is a problem it is handy to capture a snapshot of the state of things for trouble shooting purposes.  In the Nuix Workbench GUI we can generate a diagnostics file with the click of a button.  When using the Java engine API we have to do a bit more work.  This class provides a method `saveDiagnostics`.  The method accepts as an argument the directory (as a String or java.io.File) to which the diagnostics zip will be saved.  The generated zip file will automatically be given a time stamped name in the form `NuixEngineDiagnostics-YYYYMMDDHHmmss.zip`, for example `NuixEngineDiagnostics-201811325100354.zip`.

In this example `NuixDiagnostics.saveDiagnostics` is called when an exception bubbles all the way up to our main method.

```java
public static void main(String[] args) throws Exception {
	EngineWrapper wrapper = new EngineWrapper("D:\\engine-releases\\7.6.5");
	
	try {
		wrapper.withDongleLicense(new Consumer<Utilities>(){
			public void accept(Utilities utilities) {
				// ...
			}
		});

	} catch (Exception e) {
		logger.error("Unhandled exception",e);
		// Lets dump a diagnostics file since something went wrong and having
		// this may be helpful for trouble shooting
		NuixDiagnostics.saveDiagnostics("C:\\EngineDiagnostics");
	}
}
```

# License

```
Copyright 2018 Nuix

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