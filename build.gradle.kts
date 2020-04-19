group = "com.pjcampi.gradle.thrift"
version = "0.1.0"

plugins {
    kotlin("jvm") version "1.3.61"
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit:junit:4.12")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

gradlePlugin {
    (plugins) {
        register("thrift") {
            id = "$group.thrift-build-plugin"
            implementationClass = "$group.ThriftBuildPlugin"
            description = "Plugin to compile thrift schemas into a build"
        }
    }
}
