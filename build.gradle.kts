plugins {
	java
	id("org.springframework.boot") version "3.5.8"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "Concurrency_issue_simulator"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	/* ===============================
       🔹 Spring Boot 핵심 스타터
       =============================== */
	// Spring Web
	implementation("org.springframework.boot:spring-boot-starter-web")
	// Spring Data JPA
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Redis
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	// Redisson
	implementation("org.redisson:redisson-spring-boot-starter:3.52.0")
	/* ===============================
       🔹 DB 드라이버
       =============================== */
	// MySQL JDBC 드라이버
	runtimeOnly("com.mysql:mysql-connector-j")


	/* ===============================
       🔹 개발 편의 기능
       =============================== */
	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	// Devtools
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	/* ===============================
       테스트 관련 의존성
       =============================== */
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
