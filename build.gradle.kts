plugins {
    id("java")
    id("io.qameta.allure") version "2.11.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor ("org.projectlombok:lombok:1.18.38")
    implementation("ch.qos.logback:logback-classic:1.5.11")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")

}

tasks.test {
    useJUnitPlatform()
    systemProperty("env", project.findProperty("env") ?: "dev")
    systemProperty("API_TOKEN", System.getenv("API_TOKEN"))
    systemProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"))
}

allure {
    // В новых версиях Kotlin DSL используется adapter
    adapter {
        autoconfigure.set(true)  // включаем автонастройку
        aspectjVersion.set("1.9.9.1")
    }
    version.set("2.21.0")
}