plugins {
    id("java")
    id("org.springframework.boot") version "4.0.9"
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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.9"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.clickhouse:clickhouse-jdbc:0.9.8:all")
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.register<Jar>("generatorJar") {
    archiveFileName.set("generator.jar")
    from(sourceSets["main"].output)
    include("org/itmo/generator/**")
    manifest {
        attributes("Main-Class" to "org.itmo.generator.DataGen")
    }
}

tasks.register<JavaExec>("generateData") {
    group = "data generation"
    description = "Generates CSV datasets into data/"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.itmo.generator.DataGen")
    args(layout.projectDirectory.dir("data").asFile.absolutePath)
}
