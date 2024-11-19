plugins {
    id("application")
}

group = "io.github.cichlidmc"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://mvn.devos.one/snapshots")
}

dependencies {
    implementation("io.github.cichlidmc:PistonMetaParser:2.0.2")
    implementation("io.github.cichlidmc:TinyJson:1.0.1")
}

application {
    mainClass = "io.github.cichlidmc.pistonmetatracker.Main"
}
