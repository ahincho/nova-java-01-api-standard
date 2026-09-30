plugins {
    id("java-library")
    id("maven-publish")
    id("info.solidsoft.pitest") version "1.19.0-rc.1"
    jacoco
    checkstyle
    id("net.nemerosa.versioning") version "4.0.1"
    id("signing")
    id("org.owasp.dependencycheck") version "12.2.2"
    id("org.cyclonedx.bom") version "3.2.4"
}

versioning {
    releaseMode = "snapshot"
    displayMode = "snapshot"
    releaseBuild = false
}

group = "pe.edu.nova.java.libs"
version = findProperty("version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val junitVersion = "6.0.0"
val jqwikVersion = "1.9.3"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitVersion")
    testImplementation("net.jqwik:jqwik:$jqwikVersion")
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
        )
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all", "-quiet")
        encoding = "UTF-8"
        charSet = "UTF-8"
    }
}

checkstyle {
    // Only lint production code. Test suites commonly rely on static-import
    // wildcards (org.junit.jupiter.api.Assertions.*, net.jqwik.api.*), which
    // is an accepted convention that would otherwise trip AvoidStarImport.
    sourceSets = listOf(project.sourceSets.main.get())
}

dependencyCheck {
    // NVD_API_KEY / NOVA_OWASP_FAIL_ON_CVSS are injected by reusable-owasp-check.yml.
    // Locally (no env vars set) this defaults to "never fail" (11.0, matches plugin default)
    // and an empty NVD key (slower updates, acceptable for local dev).
    failBuildOnCVSS = (System.getenv("NOVA_OWASP_FAIL_ON_CVSS") ?: "11").toFloat()
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
    // Solo lo que recibe el consumidor, como en el toolchain de Nova (ADR-044): Checkstyle, PIT y las
    // pruebas no viajan con la librería, y el package-lock.json es de los hooks de commit, no del código.
    scanConfigurations.set(listOf("compileClasspath", "runtimeClasspath"))
    scanSet.setFrom(file("src/main/java"))
}

pitest {
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(setOf("pe.edu.nova.java.libs.api.standard.*"))
    targetTests.set(setOf("pe.edu.nova.java.libs.api.standard.*"))
    mutators.set(setOf("DEFAULTS"))
    outputFormats.set(setOf("HTML", "XML"))
    pitestVersion.set("1.17.4")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-01-api-standard")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    val gpgKeyId: String? = System.getenv("GPG_SIGNING_KEY_ID")
    val gpgKey: String? = System.getenv("GPG_SIGNING_KEY")
    val gpgPassword: String? = System.getenv("GPG_SIGNING_PASSWORD")

    if (gpgKeyId != null && gpgKey != null) {
        useInMemoryPgpKeys(gpgKeyId, gpgKey, gpgPassword ?: "")
        sign(publishing.publications)
    }
}