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
- Ensure that the Windows `PATH` environment variable references the `bin` sub directory of your engine release.  A restart of your machine may be necessary to ensure the updated `PATH` value is picked up properly.
- Build the project, executing the `main` method of the class `EntryPoint`.

# Code Overview

In the example we start at the `main` method of the `EntryPoint` class.  The method begins by creating a new instance of the `EngineWrapper` class, providing it the root directory of the engine release we downloaded earlier.  At this point not much has happened.  Its once we call `withDongleLicense` or `withServerLicense` that the `EngineWrapper` class gets to work.

## EngineWrapper

The workflow of `withDongleLicense` and `withServerLicense` are basically the same with the exception that one instructs Nuix to look for a dongle and the other tell Nuix to connect to a Nuix Management Server with a particular user name and password.  In the following I'll focus on `withDongleLicense` and note differences in behavior by `withServerLicense`.

The method begins by ensuring the specified engine release directory does indeed exist since nothing will work without this.  The method then loads the properties in the embedded resource `log4j.properties`.  This file contains various properties related to how [Log4j](https://github.com/apache/log4j) should be configured.  This file is essentially the same as the one included with a Nuix Workbench install at `/config/log4j.properties`.  The method then proceeds to construct a `GlobalContainer` instance, of which there should only ever be 1 per Java Virtual Machine.

From the `GlobalContainer` instance an `Engine` instance is constructed.  From the `Engine` instance a `Licensor` is obtained.  In the case of `withDongleLicense`, `Licensor.findAvailableLicences` is called with the setting `sources` having a value of `dongle`.  `withServerLicence` would instead call `Licensor.findAvailableLicences` with the setting `sources` having a value of `server`.  `Licensor.findAvailableLicences` returns an `Iterable<AvailableLicence>`.  This is where we can begin to inspect the licenses available.  The `EngineWrapper` class makes use of a `LicenseFilter` object to test each license for some requirements such as having a certain number of workers available, certain license type or particular features.  The `EngineWrapper` defaults to an instance of `LicenseFilter` which will approve any license that is available.

Once a license has been found which is available and passes requirements of the license filter, its is acquired by calling `AvailableLicense.acquire`, making your engine instance licensed and that license exclusive to you application.  At this point `withDongleLicense` creates and instance of the Nuix `Utilities` object and provides it to the `accept` method of the `Consumer<Utilities>` object passed to the method.  This is where your code can begin working with the Nuix API, creating cases, processing data, etc.

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
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():74 - License Features:
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] OTHER_EMAIL
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] LOG_STASH
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] NETWORK_DATA
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] OUTLOOK
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] GENERAL_DATA
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] DESKTOP
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] OUTLOOK_EXPRESS
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] LOTUS_NOTES
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] GRAPH
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] IMAP_POP
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] CYBER_CONTEXT
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] ANALYSIS
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] SCRIPTING
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] EXPORT_SINGLE_ITEM
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] EXPORT_ITEMS
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] EXPORT_VIEW
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] OCR_PROCESSING
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] WORKER_SCRIPTING
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] MOBILE_DEVICE_IMAGING
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] CASE_CREATION
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] PRODUCTION_SET
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] PARTIAL_LOAD
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] EXPORT_LEGAL
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] ELASTIC_SEARCH
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] EXPORT_CASE_SUBSET
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] EXCHANGE_WS
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] FAST_REVIEW
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] AUTOMATIC_CLASSIFIER_EDITING
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] WORKER
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [X] METADATA_IMPORT
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] UNRESTRICTED_CASE_ACCESS
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] SYMANTEC_VAULT
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] MAIL_XTENDER
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] AXS_ONE
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] ZANTAZ
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] SOCIAL_MEDIA
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] LIGHT_SPEED
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] GWAVA
2018-11-09 15:37:45 INFO  LicenseFeatures:logFeaturesOfLicense():78 - [ ] AOS_DATA
```

## ThirdPartyDependencyChecker

Once a license has been obtained by `EngineWrapper` and a `Utilities` object has been obtained from the now licensed `Engine` instance, this class logs third party dependency information via a call to `ThirdPartyDependencyChecker.logAllDependencyInfo(Utilities)`.  The output of which looks like this:

```
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logAllDependencyInfo():25 - Reviewing third party dependency statuses:
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [X] 'Lotus Notes': Found Version 9.0.1
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [ ] 'Microsoft Access': Not found
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [X] 'Microsoft Word': Found PDF Capable Word
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [X] 'Microsoft Excel': Found PDF Capable Excel
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [X] 'Microsoft PowerPoint': Found PDF Capable PowerPoint
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [ ] 'Microsoft Visio': Not found
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [ ] 'FFmpeg / FFprobe': Not found
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [X] 'Relativity': Found
2018-11-09 15:37:45 INFO  ThirdPartyDependencyChecker:logDependencyInfo():38 - [ ] 'Nuix OCR': Not found
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