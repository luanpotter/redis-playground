plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.serialization") version "2.0.21"
  application
  id("io.gitlab.arturbosch.detekt") version "1.23.6"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}
repositories { mavenCentral() }
dependencies {
  implementation("io.ktor:ktor-server-core-jvm:3.0.1")
  implementation("io.ktor:ktor-server-netty-jvm:3.0.1")
  implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.1")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("ch.qos.logback:logback-classic:1.5.8")
  testImplementation(kotlin("test"))
}
kotlin { jvmToolchain(21) }
application { mainClass.set("app.source.MainKt") }
detekt {
  config.setFrom(files(rootProject.file("../../detekt.yml")))
  buildUponDefaultConfig = true
}
ktlint {
  version.set("1.3.1")
}
