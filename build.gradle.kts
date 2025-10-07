plugins {
    id("java")
    id("io.qameta.allure") version "2.11.2"
}

repositories {
    mavenCentral()
}

dependencies {
    // Test framework
    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")

    // Logging - SLF4J + Logback only (removed Log4j dependencies)
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.11")

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

    // Allow SystemLambda to modify environment variables on JDK 21+
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )
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
