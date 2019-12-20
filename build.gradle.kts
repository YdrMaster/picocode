import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/jcenter")
    }
}

plugins {
    kotlin("jvm") version "1.3.61"
}

// 包括主项目的构建脚本
allprojects {
    apply(plugin = "kotlin")
    group = "cn.autolabor"
    version = "v0.0.1"
    repositories {
        mavenCentral()
        jcenter()
    }
    dependencies {
        // 自动依赖 kotlin 标准库
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.1.1")
        // 单元测试
        testImplementation("junit", "junit", "+")
        testImplementation(kotlin("test-junit"))
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions { jvmTarget = "1.8" }
    }
    tasks.withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    // 源码导出任务
    with("sourcesJar") {
        tasks["jar"].dependsOn(this)
        tasks.register<Jar>(this) {
            group = JavaBasePlugin.BUILD_TASK_NAME
            description = "create sources jar"
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }
    }
}

// 主项目依赖项
dependencies {
    implementation("net.java.dev.jna", "jna", "+")
    implementation("org.bytedeco", "opencv-platform", "+")
}
