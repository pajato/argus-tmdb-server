plugins {
    kotlin("multiplatform") version "1.3.31"
    `maven-publish`
    jacoco
}

// Note to any reviewer: what's the "right" way to do this, handle version settings?
val kotlinVersion = "1.3.31"
val coroutinesVersion = "1.1.1"
val klockVersion = "1.1.1"
val javalinVersion = "2.8.0"
val slf4jVersion = "1.7.26"
val argusCoreVersion = "0.0.8"

group = "com.pajato"
version = "0.0.1"

repositories {
    jcenter()
    mavenCentral()
    maven( "https://dl.bintray.com/soywiz/soywiz")
    mavenLocal()
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("com.soywiz:klock:$klockVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        jvm("jvm").compilations["main"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
                implementation("io.javalin:javalin:$javalinVersion")
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                implementation("com.pajato:argus-tmdb-core-jvm:$argusCoreVersion")
            }
        }

        jvm("jvm").compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
    }
}

tasks.register<Copy>("copyMainResources") {
    from(file("$projectDir/src/jvmMain/resources/"))
    into(file("$buildDir/classes/kotlin/jvm/main/"))
}

tasks.get(name = "jvmMainClasses").dependsOn += tasks.get(name = "copyMainResources")

jacoco {
    toolVersion = "0.8.3"
}

tasks {
    val coverage = register<JacocoReport>("jacocoJVMTestReport") {
        group = "Reporting"
        description = "Generate Jacoco coverage report."
        classDirectories.setFrom(fileTree("$buildDir/classes/kotlin/jvm/main"))
        val coverageSourceDirs = listOf("src/commonMain/kotlin", "src/jvmMain/kotlin")
        additionalSourceDirs.setFrom(files(coverageSourceDirs))
        sourceDirectories.setFrom(files(coverageSourceDirs))
        executionData.setFrom(files("$buildDir/jacoco/jvmTest.exec"))
        reports {
            html.isEnabled = true
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }
    named("jvmTest") {
        finalizedBy(coverage)
    }
}
