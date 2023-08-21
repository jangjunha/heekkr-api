import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.2"
	id("io.spring.dependency-management") version "1.1.2"
	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"
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
	implementation("com.linecorp.armeria:armeria-spring-boot3-starter:1.24.3")
	implementation("com.linecorp.armeria:armeria-grpc-kotlin:1.24.3")
	implementation("com.linecorp.armeria:armeria-grpc:1.24.3")
	implementation("com.linecorp.armeria:armeria-grpc-protocol:1.24.3")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.apache.lucene:lucene-core:9.7.0")
	implementation("org.apache.lucene:lucene-analyzers-nori:8.11.2")
	implementation("org.apache.lucene:lucene-queryparser:9.7.0")
	implementation("org.apache.lucene:lucene-highlighter:9.7.0")
	implementation("io.grpc:grpc-api:1.56.1")
	implementation("io.grpc:grpc-kotlin-stub:1.3.0")
	implementation("io.grpc:grpc-netty:1.56.1")
	implementation("com.google.protobuf:protobuf-kotlin:3.23.4")
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
