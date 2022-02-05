Nuix Java Engine Baseline
=========================

![Nuix Engine 9.6](https://img.shields.io/badge/Nuix%20Engine-9.6-green.svg)

View the GitHub project [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline) or download the latest release [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline/releases).

View the Java docs [here](https://nuix.github.io/Nuix-Java-Engine-Baseline/).

# Overview

**Written By:** Jason Wells

This repository contains a series of examples making use of the Java Engine API and can be used as a starting point for your own Nuix Java Engine based project.

# Getting Started

## Setup

This project makes use of [Gradle](https://gradle.org/) and has been tested with [IntelliJ Idea IDE](https://www.jetbrains.com/idea/).  It may work with other IDEs such as [Eclipse](https://www.eclipse.org/eclipseide/) as well, but it has only been tested in IntelliJ Idea.

[Gradle](https://gradle.org/) is a build automation tool.  While it adds a small degree of additional complexity (the [build.gradle](https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/IntelliJ/build.gradle) file is not the most straightforward) it does provide some benefits that make it worth the additional complexity.

1. If you do not already have it installed, download and install [IntelliJ Idea IDE](https://www.jetbrains.com/idea/download/#section=windows) (the community edition should be fine).
1. Download or clone this repository to your development machine.
1. Download a Nuix Java Engine API release zip file and extract its contents to the `engine` sub-directory of your local copy.
1. Start IntelliJ Idea and open project by selecting `\Nuix-Java-Engine-Baseline\IntelliJ\build.gradle`.
1. Import process should begin.  This can take a few minutes as dependencies are pulled down from the internet, dependencies are indexed, etc.

# Basic Overview

In the various examples, we start by executing the `main` method of the relevant class.  The method begins by creating a new instance of the [EngineWrapper] class, providing it the root directory of the engine release.  From there we obtain a licensed instance of the Engine by calling [withDongleLicense], [withServerLicense] or [withCloudLicense] method of the [EngineWrapper] class.

Note that many of the classes (such as [EngineWrapper]) provided in this project are not required to use the engine API.  Instead they provide a demonstration of how you might approach license acquisition, engine initialization, etc.

## EngineWrapper

The workflow of [withDongleLicense], [withServerLicense] and [withCloudLicense] are fairly similar in the steps they take, differing mostly in how they acquire a license for the engine instance they obtain.  In the following I'll focus on [withDongleLicense] and note some of the differences in behavior to [withServerLicense] and [withCloudLicense].

The method [withDongleLicense] begins by ensuring the specified engine release directory does indeed exist since nothing will work without this.  The method then proceeds to construct a `GlobalContainer` instance (as needed).  There should only ever be 1 `GlobalContainer` instance per Java Virtual Machine.

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

`Licensor.findAvailableLicencesStream` returns an `Stream<AvailableLicence>`.  The stream is then filtered to `AvailableLicence` instances for which the `LicenseFilter.isValid` method returns **true**.  This tests each license for some requirements such as having a certain number of workers available, being a certain license type or having particular features.  The default [LicenseFilter] provided by [EngineWrapper] will acquire the first license it finds available, but it can be configured to be more selective.  The first matching license is then acquired, taking as many workers as are specified by `LicenseFilter.getMinWorkers`.

```java
logger.info("Finding first license which meets filter requirements...");
Stream<AvailableLicence> licences = licensor.findAvailableLicencesStream(licenseOptions);
boolean licenceObtained = false;
Optional<AvailableLicence> possiblyFoundLicense = licences.filter(availableLicence -> licenseFilter.isValid(availableLicence)).findFirst();
if(possiblyFoundLicense.isEmpty() == false) {
	AvailableLicence foundLicense = possiblyFoundLicense.get();
	logger.info(">>>> Acquiring following license: "+LicenseFeaturesLogger.summarizeLicense(foundLicense));
	if(foundLicense.canChooseWorkers()) {
		int targetWorkerCount = licenseFilter.getMinWorkers();
		if(targetWorkerCount < 1) { targetWorkerCount = 2; }
		Map<String,Object> acquireSettings = new HashMap<String,Object>();
		acquireSettings.put("workerCount", targetWorkerCount);
		foundLicense.acquire(acquireSettings);
	} else {
		foundLicense.acquire();
	}
	licenceObtained = true;
} else {
	logger.warn("!!!! No license found that matches "+licenseFilter.toString());
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
EngineWrapper wrapper = new EngineWrapper(System.getProperty("nuix.engineDir"),System.getProperty("nuix.logDir"));

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
2022-02-04 11:35:30 INFO  LicenseFeatures:logFeaturesOfLicense():83 - License Features:
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
2022-02-04 11:35:45 INFO  ThirdPartyDependencyChecker:logAllDependencyInfo():38 - Reviewing third party dependency statuses:
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

When there is a problem, it is helpful to capture a snapshot of the state of things for trouble shooting purposes.  In the Nuix Workbench GUI we can generate a diagnostics file with the click of a button.  When using the Java engine API we have to do a bit more work.  The class [NuixDiagnostics] provides a method [saveDiagnosticsToDirectory](https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/NuixDiagnostics.html#saveDiagnosticsToDirectory-java.lang.String-) which can generate a Nuix diagnostics file for you.  The method accepts as an argument the directory (as a String or [java.io.File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)) to which the diagnostics zip will be saved.  The generated zip file will automatically be given a time stamped name in the form `NuixEngineDiagnostics-yyyyMMddHHmmss.zip`, for example `NuixEngineDiagnostics-20220204113545.zip`.

In the example [saveDiagnosticsToDirectory](https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/NuixDiagnostics.html#saveDiagnosticsToDirectory-java.lang.String-) is called when an exception bubbles all the way up to our main `try`/`catch`.

```java
public static void main(String[] args) throws Exception {
	EngineWrapper wrapper = new EngineWrapper(System.getProperty("nuix.engineDir"),System.getProperty("nuix.logDir"));
	
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
		NuixDiagnostics.saveDiagnosticsToDirectory("C:\\EngineDiagnostics");
	}
}
```

# Running an Example

A Gradle task is defined for running each example.  To run an example:
1. Expand the **Gradle** tab (likely along the right side of the IDE).
1. Expand **Nuix-Java-Engine-Baseline** -> **Tasks** -> **examples**.
1. Double-click one of the tasks to run the corresponding example (**BasicInitialization** is a good initial test).

# Gradle Build File

The `plugins` section allows us to include plugins that provide functionality to our build file.  We include the `java` plugin which provides various functionality for a Java project.

```groovy
plugins {
    id 'java'
}
```

The `repositories` section allows use to define where dependencies may be resolved from.  Note that in addition to specifying dependencies be resolved from Maven, we also specify the `lib` sub-directory of the Nuix Java Engine release we have downloaded.

```groovy
repositories {
    mavenCentral()
    flatDir { dirs 'engine/lib' }
}
```

In the `dependencies` section we declare some dependencies as well as whether they are needed during development/compilation (`implementation`), or just on the class path while executing (`runtimeOnly`), etc.  Note that we include Nuix scripting and engine jars from the `lib` sub-directory of the Nuix Java Engine release we have downloaded for development time and all the `lib` jars for run-time.

```groovy
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.2'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.2'

    // Used for logging
    implementation 'org.apache.logging.log4j:log4j-api:2.17.1'
    implementation 'org.apache.logging.log4j:log4j-core:2.17.1'

    // Required for various date objects
    implementation 'joda-time:joda-time:2.10.13'

    // Reference Nuix dependencies
    implementation fileTree(dir: 'engine/lib', include: 'nuix-scripting*.jar')
    implementation fileTree(dir: 'engine/lib', include: 'nuix-engine*.jar')
    runtimeOnly fileTree(dir: 'engine/lib', include: '*.jar')
}
```

In the following section we specify that we need to use a version of Java that supports the Java 11 language specification.

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}
```

Next we define some values that we will make use of in the rest of the file:
- `engineDirectory`: The location of out engine release, relative to the project directory (the `engine` sub-directory).
- `engineBinDirectory`, `engineX86BinDirectory`: Two directories within the engine release sub-directory that need to be on the `PATH` environment variable at run-time.
- `rootLogDir`: The location we will direct logs to be generated in, relative to the project directory (the `logs` sub-directory).
- `envPath`: The `PATH` environment variable we will inject into the JVM when running tests.  The engine and worker processes need to be able to resolve some binaries on the `PATH`.  This allows us to define a custom `PATH` value for our JVM that will point to the appropriate engine directories (relative to this project) without needing to alter the machine's actual `PATH` environment variable.

```groovy
String engineDirectory = rootProject.rootDir.getAbsolutePath() + "\\engine"
String engineBinDirectory = engineDirectory + "\\bin"
String engineX86BinDirectory = engineDirectory + "\\bin\\x86"
String rootLogDir = rootProject.rootDir.getAbsolutePath() + "\\logs"
String envPath = engineBinDirectory + ";" + engineX86BinDirectory
```

We define a series of JVM arguments we want to be used each time we run our code in a JVM.  These arguments define some important things such as engine directory, log directory, accessible temp directory.

```groovy
def defaultJvmArgs = [
        // Necessary for newer versions of Nuix.  Without this you will likely see
        // an error regarding loading the BouncyCastle crypto library at run-time
        "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",

        // This will be passed to EngineWrapper on creation allowing it to resolve
        // all the dependencies in an engine release
        "-Dnuix.engineDir=\"${engineDirectory}\"",

        // Provided to EngineWrapper to specify the root directory where logs will be written
        "-Dnuix.logDir=\"${rootLogDir}\"",

        // Specify a temp directory accessible to the user running our code
        "-Djava.io.tmpdir=\"${System.getenv("LOCALAPPDATA")}\\Temp\\Nuix\"",
]
```

Beyond that, a series of [JavaExec tasks](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.JavaExec.html) are defined, each which provides a way to run one of the examples.

# License

```
Copyright 2022 Nuix

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
[withCloudLicense]: https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/IntelliJ/src/main/java/com/nuix/javaenginesimple/EngineWrapper.java#L243
[BasicInitializationExample]: https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/IntelliJ/src/main/java/com/nuix/javaenginesimple/examples/BasicInitializationExample.java
[NuixDiagnostics]: https://nuix.github.io/Nuix-Java-Engine-Baseline/com/nuix/javaenginesimple/NuixDiagnostics.html