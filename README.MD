Nuix Java Engine Baseline
=========================

![Nuix Engine 9.10.x.x](https://img.shields.io/badge/Nuix%20Engine-9.10.x.x-green.svg)

View the GitHub project [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline) or download the latest release [here](https://github.com/Nuix/Nuix-Java-Engine-Baseline/releases).

View the Java docs [here](https://nuix.github.io/Nuix-Java-Engine-Baseline/).

# Overview

**Written By:** Jason Wells

This repository contains a series of examples making use of the Java Engine API and can be used as a foundation for your own Nuix Java Engine based project.  This project provides implementation of some commonly desired functionality one may want around using the Nuix Engine API:

- Environment configuration checks with hints at addressed misconfigurations
- License resolution with fallbacks when initially desired license is not available
- Logging initialization
- Diagnostics generation
- Third party dependency checking

This project can provide you a quick way to get up and running with some additional functionality built on top.  Due to some of the extra features included, this project may not be the easiest way to wrap you head around the fundamental steps needed to use the Nuix Java Engine API.  If you are looking for a more straight forward example demonstrating core concepts regarding getting the Nuix Java Engine API up and running, see the [Java SDK Starter repository](https://github.com/Nuix/java-sdk-starter).

# Setup

## Nuix Java Engine Distributable

We will need to obtain a Nuix Java Engine API distributable release.  This contains the various binaries and dependencies needed to run the Nuix Java Engine API.

1. Download an Engine release from the [Nuix download site](download.nuix.com).
1. Extract its contents to a directory, such as `C:\EngineRelease`.
1. Edit the system environment variables, adding a new variable named `NUIX_ENGINE_DIR` assigning the directory in the previous step as the value.  The project refers to this environment variable to resolve these dependencies.
1. (Optional) If you wish to obtain your license from either Nuix Management Server (NMS) or Nuix Cloud License Server (CLS) you may want to specify authentication credentials.  The project supports obtaining the user name and password from environment variables `NUIX_USERNAME` and `NUIX_PASSWORD` (although does not require this).

## IDE

This project makes use of [Gradle](https://gradle.org/) and has been tested with [IntelliJ Idea IDE](https://www.jetbrains.com/idea/).  It may work with other IDEs such as [Eclipse](https://www.eclipse.org/eclipseide/) as well, but it has only been tested in IntelliJ Idea.

[Gradle](https://gradle.org/) is a build automation tool.  While it adds a small degree of additional complexity (the [build.gradle.kts](https://github.com/Nuix/Nuix-Java-Engine-Baseline/blob/master/IntelliJ/build.gradle.kts) file is not the most straightforward) it does provide some benefits that make it worth the additional complexity.

1. If you do not already have it installed, download and install [IntelliJ Idea IDE](https://www.jetbrains.com/idea/download/#section=windows) (the community edition should be fine).
1. Download or clone this repository to your development machine.
1. Download a Nuix Java Engine API release zip file and extract its contents to the `engine` sub-directory of your local copy.
1. Start IntelliJ Idea and open project by selecting `\Nuix-Java-Engine-Baseline\IntelliJ\build.gradle.kts`.
1. Import process should begin.  This can take a few minutes as dependencies are pulled down from the internet, dependencies are indexed, etc.

# NuixEngine Class

Making use of the Nuix Java Engine API, at a high level, involves:

1. Ensure environment is configured appropriately, such as having Nuix Engine dependencies in place.
1. Start engine instance.
1. License the engine instance.
1. Once licensed, make use of the `Utilities` object to interact with the rest of the API.

This repository provides much of the logic to perform this work, allowing you to get the engine initialized as simply as:

```java
// Define a resolver which will resolve licenses from Cloud License Server (CLS),
// authenticating using upon environment variable "NUIX_USERNAME" and "NUIX_PASSWORD",
// that have at least 4 workers and the feature "CASE_CREATION".
LicenseResolver cloud_4_workers = NuixLicenseResolver.fromCloud()
    .withLicenseCredentialsResolvedFromEnvVars()
    .withMinWorkerCount(4)
    .withRequiredFeatures("CASE_CREATION");

// Define a resolver which will attempt to resolve a license from a local physical dongle
// that has the feature "CASE_CREATION".
LicenseResolver anyDongle = NuixLicenseResolver.fromDongle()
    .withRequiredFeatures("CASE_CREATION");

// Create a new NuixEngine instance which will first attempt to resolve a cloud license and then
// attempt to resolve a dongle license if one cannot be resolved from cloud, using resolvers
// defined above.  Calling run method to execute code with a licensed Engine instance (if a license can be obtained).
NuixEngine.usingFirstAvailableLicense(cloud_4_workers, anyDongle)
    .setEngineDistributionDirectoryFromEnvVars()
    .run((utilities -> {
        log.info("License was obtained!");
        // Do something with Utilities/API here
    }));
```

In the example above, the `run` method will internally call the `NuixEngine.getUtilities` method.  This in turn will do the following:
1. Call `NuixEngine.checkPreconditions` which will check some environmental settings, using sensible defaults when possible and throwing informative errors when it cannot.
1. Initialize log4j2 logging for you.
1. Constructuct `GlobalContainer` and `Engine` instances for you with some lifetime management hooked into JVM shut down.
1. Work through the 1 or more `LicenseResolver` instances provided to resolve a license, logging information about the process.
1. Log information regarding presence of third party dependencies.
1. Yield a licensed instance of `Utilities` for you to work with, if a license was able to be obtained.

# Running an Example Outside Gradle

Gradle simplifies running the examples, but what is needed to run an example (or your own code) outside of Gradle?  We need to take similar steps to what Gradle would be doing:
- Start a JVM (Java Virtual Machine) to run the program.
- Make sure that the engine release sub-directories `bin` and `bin\x86` can be resolved via the `PATH` environment variable.
- Make sure your JAR can be resolved on the JVM classpath.
- Make sure the Nuix dependency JAR files in the engine release `lib` sub-directory can be resolved on the JVM classpath.
- Make sure we know the entry point to our program.  The entry point is the Java class containing the `public static void main(String[] args)` method to run.

## Make Sure Java is Installed
It is assumed that you have Java installed and that running the command `java` on the console will succeed.

## `PATH` References to `bin` and `bin/x86`
You will need to make sure that the `PATH` environment variable for the JVM process points to the engine release sub-directories `bin` and `bin\x86`.  This can be accomplished different ways.  The easiest is to add those paths to the `PATH` environment variable.  There are ways to set these temporarily for the JVM process you start.  For example you could use a batch file and `SET LOCAL` / `END LOCAL` ([doc](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/setlocal)) in combination with `SET` ([doc](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/set_1)) or start your program via something like the .NET class [Process](https://learn.microsoft.com/en-us/dotnet/api/system.diagnostics.process?view=net-7.0) which allows for customizing the environment variables just for a process it starts via 

```csharp
Process nuixProcess = new Process();
...
...
string engineDir = "C:\\EngineRelease";
string engineBinDir = engineDir + "\\bin";
string enginex86BinDir = engineBinDir + "\\x86";
string existingPath = Environment.GetEnvironment("PATH");
nuixProcess.StartInfo.EnvironmentVariables.Add("PATH", existingPath+";"+engineBinDir+";"+enginex86BinDir );
```

## Construct the Command

The command takes the basic form:

```
java --add-exports=java.base/jdk.internal.loader=ALL-UNNAMED -cp "<Engine Lib Directory>/*;<Path to Jar>" <Main Class>
```

If my engine release `lib` directory is located at `C:\EngineRelease\lib`, my compiled JAR is located at `C:\MyApp\MyCustomNuixApp-v1.0.0.jar` and my `public static void main(String[] args)` method exists in a class `com.company.CustomNuixApp` then the command would look like this:

```
java --add-exports=java.base/jdk.internal.loader=ALL-UNNAMED -cp "C:/EngineRelease/lib/*;C:/MyApp/MyCustomNuixApp-v1.0.0.jar" com.company.CustomNuixApp
```

Some things to note:

- The argument `--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED` is necessary and you will have startup issues if you do not include this.  Note that `NuixEngine` class will check for this during check of pre-conditions.
- The class path references use forward slashes (`/`) rather than the Windows norm of using back slashes (`\`).
- The engine lib directory class path reference ends with `/*` to include all JAR files in that directory.
- The program JAR reference is an absolute reference to the JAR file (it could be reference to its containing directory though).
- The class path entries are delimited with a semicolon (`;`).
- The *last argument* is the fully qualified name (package and class name) of the class containing the entry point we are running.

# Where to Get Help

Having trouble getting things up and running?  Here are several places you can seek assistance:

- Create a [new Issue](https://github.com/Nuix/Nuix-Java-Engine-Baseline/issues/new)
- Ask on the [Nuix Community Forums](https://forums.nuix.com/)
- Contact [Nuix Support](https://nuix.service-now.com/support)

# License

```
Copyright 2023 Nuix

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