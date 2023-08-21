import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.2"
	id("io.spring.dependency-management") version "1.1.2"
	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
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
	implementation("io.grpc:grpc-netty:$grpcVersion")
	implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
	implementation("kr.heek:proto:1.3.0")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
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
