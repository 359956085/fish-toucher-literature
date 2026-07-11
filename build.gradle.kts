import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.novelreader"
version = "3.6.3"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    intellijPlatform {
        // Build 253 = IDEA 2025.3
        // 如果 intellijIdea("2025.3") 报错, 可替换为下面的精确 build number:
        //   intellijIdea("2025.3.2")
        //   intellijIdea("253.30387.90")
        // 或者使用 maven snapshot (不走 installer):
        //   intellijIdea("253-EAP-SNAPSHOT") { useInstaller = false }
        intellijIdea("2026.1.1")

        pluginVerifier()
        zipSigner()
        // instrumentationTools()  // 不需要 forms/NotNull instrumentation，禁用避免解析问题
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2024.2")
            create(IntelliJPlatformType.IntellijIdea, "2026.1.1")
        }
    }

    buildSearchableOptions = false
    instrumentCode = false
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform {
            excludeTags("large-file")
        }
    }

    register<Test>("largeFileTest") {
        group = "verification"
        description = "验证 500 MiB 小说文件的磁盘索引加载"
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        maxHeapSize = "512m"
        useJUnitPlatform {
            includeTags("large-file")
        }
    }
}
