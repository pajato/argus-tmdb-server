val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val coroutinesVersion = "1.1.1"
val klockVersion = "1.1.1"
val argusCoreVersion = "0.0.11"

plugins {
    application
    kotlin("jvm") version "1.3.40"
    jacoco
}

group = "com.pajato.argus"
version = "0.1.0"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    jcenter()
    maven { url = uri("https://dl.bintray.com/soywiz/soywiz") }
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("ch.qos.logback:logback-classic:$logbackVersion")
    compile("io.ktor:ktor-server-core:$ktorVersion")
    compile("io.ktor:ktor-html-builder:$ktorVersion")
    compile("io.ktor:ktor-server-host-common:$ktorVersion")
    compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    compile("com.soywiz:klock-jvm:$klockVersion")
    compile("com.pajato.argus:argus-tmdb-core-jvm:$argusCoreVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testCompile("io.ktor:ktor-server-tests:$ktorVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit")
    testCompile("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

jacoco {
    toolVersion = "0.8.4"
}
