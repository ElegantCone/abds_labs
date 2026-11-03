plugins {
    id("java")
}

group = "org.itmo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("generateData") {
    group = "data generation"
    description = "Generates CSV datasets into data/"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.itmo.generator.DataGen")
    args(layout.projectDirectory.dir("data").asFile.absolutePath)
}
