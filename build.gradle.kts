plugins {
  id("java")
  kotlin("jvm") version "2.2.10"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "tech.snaco"
version = "3.1.1+1.21"

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
  compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
  compileOnly("org.apache.commons:commons-lang3:3.18.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
}

tasks.getByName<Test>("test") {
  useJUnitPlatform()
}

tasks {
  runServer {
    minecraftVersion("1.21.8")
  }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
  javaLauncher = javaToolchains.launcherFor {
    vendor = JvmVendorSpec.JETBRAINS
    languageVersion = JavaLanguageVersion.of(21)
  }
  jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks {
  shadowJar {
    archiveClassifier.set("")
    exclude("META-INF/*.kotlin_module")
  }
  build { dependsOn(shadowJar) }
}
