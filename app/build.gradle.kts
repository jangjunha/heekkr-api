import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.2"
	id("io.spring.dependency-management") version "1.1.2"
	id("com.google.cloud.tools.jib") version "3.3.2"
	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
	kotlin("plugin.serialization") version kotlinVersion
}

buildscript {
	dependencies {
		classpath("com.google.cloud.tools:jib-spring-boot-extension-gradle:0.1.0")
	}
}

group = "kr.heek"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://asia-northeast3-maven.pkg.dev/heekkr/heekkr-maven/")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	annotationProcessor("org.springframework:spring-context-indexer")
	implementation("com.linecorp.armeria:armeria-spring-boot3-starter:$armeriaVersion")
	implementation("com.linecorp.armeria:armeria-grpc-kotlin:$armeriaVersion")
	implementation("com.linecorp.armeria:armeria-grpc:$armeriaVersion")
	implementation("com.linecorp.armeria:armeria-grpc-protocol:$armeriaVersion")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.apache.lucene:lucene-core:$luceneVersion")
	implementation("org.apache.lucene:lucene-analyzers-nori:8.11.2")
	implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")
	implementation("org.apache.lucene:lucene-highlighter:$luceneVersion")
	implementation("io.grpc:grpc-api:$grpcVersion")
	implementation("io.grpc:grpc-auth:$grpcVersion")
	implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
	implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
	implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
	implementation("io.ktor:ktor-client-core:$ktorVersion")
	implementation("io.ktor:ktor-client-cio:$ktorVersion")
	implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
	implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
	implementation("kr.heek:proto:1.3.0")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

jib {
	from {
		image = "bellsoft/liberica-openjre-alpine:17"
	}
	to {
		image = "asia-northeast3-docker.pkg.dev/heekkr/heekkr-docker/heekkr-api"
	}
	pluginExtensions {
		pluginExtension {
			implementation = "com.google.cloud.tools.jib.gradle.extension.springboot.JibSpringBootExtension"
		}
	}
}
