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