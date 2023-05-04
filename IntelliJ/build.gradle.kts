plugins {
    id("java")
}

group = "com.nuix.enginebaseline"
version = "9.10.9.584"

val sourceCompatibility = 11
val targetCompatibility = 11

// Directory where the Nuix Engine is located - used as a root for other directories and passed
// to runtimes as an environment variable.
val nuixEngineDirectory: String = System.getenv("NUIX_ENGINE_DIR")
if(nuixEngineDirectory.isNullOrEmpty()){
    throw InvalidUserDataException("Please populate the environment variable 'NUIX_ENGINE_DIR' with directory containing a Nuix Engine release")
}
val engineLibDir = "${nuixEngineDirectory}\\lib"
// Nuix engine distributable dirs "bin" and "bin/x86" need to be on the PATH for engine
// to work correctly so we will define the directories relative to this project then set below
val engineBinDir = "${nuixEngineDirectory}\\bin"
val engineBinX86Dir = "${nuixEngineDirectory}\\bin\\x86"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")

    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    testImplementation("net.datafaker:datafaker:1.9.0")

    implementation(fileTree(baseDir = engineLibDir) {
        include(
                "**/*log*.jar",
                "**/*joda*.jar",
                "**/*commons*.jar",
                // All Nuix API jars
                "**/nuix-*.jar"
        )
    })

    testRuntimeOnly(fileTree(baseDir = engineLibDir) {
        include("*.jar")
    })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

fun configTestEnv(test: Test) {
    // Define a temp directory that we should be able to write to.  Used in several places to
    // provide a temp directory that engine can use!
    val nuixTempDirectory = findProperty("tempDir") ?: "${System.getenv("LOCALAPPDATA")}\\Temp\\Nuix"

    // Necessary for newer versions of Nuix.  Without this you will likely see
    // an error regarding loading the BouncyCastle crypto library at run-time
    test.jvmArgs(
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.fs=ALL-UNNAMED",
            "--add-opens=java.base/sun.net.dns=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.fs=ALL-UNNAMED",
            "--add-opens=java.base/sun.net.dns=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
            "-Xmx4G",
            "-Djava.io.tmpdir=\"${nuixTempDirectory}\"",
    )

    // Going to pass location of test data so tests can obtain, extract and otherwise use
    // test data they may need.  For example, if your test needs a Nuix case and there is a zip
    // file containing that case hosted online, your test might first see if the zip is already present
    // in the TestData directory, if not it can download it there and reuse it on future invocations.
    val testDataDirectory = "${projectDir}\\..\\TestData"

    // Specify output directory that tests can use to store output for post test review
    val testOutputDirectory = "${projectDir}\\..\\TestOutput\\${System.currentTimeMillis()}"

    test.setEnvironment(
            // Add our engine release's bin and bin/x86 to PATH
            Pair("PATH","${System.getenv("PATH")};${engineBinDir};${engineBinX86Dir}"),

            // Define where tests can place re-usable test data
            Pair("TEST_DATA_DIRECTORY",testDataDirectory),

            // Define where tests can write output produce for later review
            Pair("TEST_OUTPUT_DIRECTORY",testOutputDirectory),

            // Forward ENV username and password
            Pair("NUIX_USERNAME",System.getenv("NUIX_USERNAME")),
            Pair("NUIX_PASSWORD",System.getenv("NUIX_PASSWORD")),

            // Forward LOCALAPPDATA and APPDATA
            Pair("LOCALAPPDATA",System.getenv("LOCALAPPDATA")),
            Pair("APPDATA",System.getenv("APPDATA")),

            // We need to make sure we set these so workers will properly resolve temp dir
            // (when using a worker based operation via EngineWrapper).
            Pair("TEMP",nuixTempDirectory),
            Pair("TMP",nuixTempDirectory),

            Pair("NUIX_ENGINE_DIR", nuixEngineDirectory)
    )
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    configTestEnv(this)
}

tasks.getByName<Javadoc>("javadoc"){
    setDestinationDir(File("${projectDir}/docs"))
}