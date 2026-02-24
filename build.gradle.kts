plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.novelreader"
version = "1.3.3"

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
        intellijIdea("2025.3")

        pluginVerifier()
        zipSigner()
        // instrumentationTools()  // 不需要 forms/NotNull instrumentation，禁用避免解析问题
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "253.*"
        }
    }

    buildSearchableOptions = false
    instrumentCode = false
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}
