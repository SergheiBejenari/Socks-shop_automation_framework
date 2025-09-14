plugins {
    id("java")
    id("io.qameta.allure") version "2.11.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Test framework
    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")

    // Logging - SLF4J + Logback only (removed Log4j dependencies)
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.11")

    // JSON processing
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    // Allure reporting
    testImplementation("io.qameta.allure:allure-testng:2.29.0")

    // Lombok (consolidated versions)
    compileOnly("org.projectlombok:lombok:1.18.40")
    annotationProcessor("org.projectlombok:lombok:1.18.40")
    testCompileOnly("org.projectlombok:lombok:1.18.40")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.40")
}

tasks.test {
    useTestNG()
// Show test logging in CI without too much noise
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

allure {
    // В новых версиях Kotlin DSL используется adapter
    adapter {
        autoconfigure.set(true)  // включаем автонастройку
        aspectjVersion.set("1.9.9.1")
    }
    version.set("2.21.0")
}

configurations.all {
    resolutionStrategy {
        // Prefer latest within minor for security patches if needed in CI
        cacheChangingModulesFor(0, "seconds")
    }
}