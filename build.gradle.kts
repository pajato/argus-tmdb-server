plugins {
    application
    kotlin("jvm") version "1.3.41"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.KOTLIN}")
    implementation("io.ktor:ktor-server-netty:${Versions.KTOR}")
    implementation("ch.qos.logback:logback-classic:${Versions.LOGBACK}")
    implementation("io.ktor:ktor-server-core:${Versions.KTOR}")
    implementation("io.ktor:ktor-html-builder:${Versions.KTOR}")
    implementation("io.ktor:ktor-server-host-common:${Versions.KTOR}")
    implementation("io.ktor:ktor-network-tls:${Versions.KTOR}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.KOTLIN}")
    implementation("com.soywiz:klock-jvm:${Versions.KLOCK}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}")

    api("com.pajato.argus:argus-tmdb-core-jvm:${Versions.ARGUS_CORE}")

    testImplementation("io.ktor:ktor-server-tests:${Versions.KTOR}")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES}")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks {
    "run"(JavaExec::class) {
        val helper = SSLHelper(project)
        environment("KEY_STORE_PATH", helper.getKeyStorePath())
        environment("KEY_STORE_PASSWORD", helper.getKeyStorePassword())
        environment("PRIVATE_KEY_PASSWORD", helper.getPrivateKeyPassword())
    }
}

jacoco {
    toolVersion = "0.8.4"
}

tasks {
    val coverage = register<JacocoReport>("jacocoJVMTestReport") {
        group = "Reporting"
        description = "Generate Jacoco coverage report."
        classDirectories.setFrom(fileTree("$buildDir/classes/kotlin/main"))
        val coverageSourceDirs = listOf("src")
        additionalSourceDirs.setFrom(files(coverageSourceDirs))
        sourceDirectories.setFrom(files(coverageSourceDirs))
        executionData.setFrom(files("$buildDir/jacoco/test.exec"))
        @Suppress("UnstableApiUsage")
        reports {
            html.isEnabled = true
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }
    named("test") {
        finalizedBy(coverage)
    }
}
