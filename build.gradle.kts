/*
 *  Village Defense - Protect villagers from hordes of zombies
 *  Copyright (c) 2023 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    id("signing")
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
}

// Windows default encoding may be GBK; force UTF-8 for Java sources/resources.
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven(uri("https://maven.plugily.xyz/releases"))
    maven(uri("https://maven.plugily.xyz/snapshots"))
    // Put PaperMC behind Plugily repos to avoid build failures on PaperMC timeouts.
    maven(uri("https://papermc.io/repo/repository/maven-public/"))
    maven(uri("https://repo.citizensnpcs.co/"))
    maven(uri("https://repo.maven.apache.org/maven2/"))
}



dependencies {
    // Use local Maven repository copy to avoid remote timeouts.
    // (F:\Maven\mavenrepository contains MiniGamesBox-Classic 1.4.5 built for Java 8)
    implementation(files("F:/Maven/mavenrepository/plugily/projects/MiniGamesBox-Classic/1.4.5/MiniGamesBox-Classic-1.4.5.jar"))
    // Target legacy runtime (Spigot 1.8.8): compile against 1.8.8 API only.
    // NOTE: do not add modern Spigot API here, otherwise incompatible calls may compile.
    // 1.8.8-only build: compile against Spigot API 1.8.8.
    // Use a local jar to avoid remote resolution (and bungeecord-chat snapshot downloads).
    compileOnly(files("F:/Maven/mavenrepository/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/spigot-api-1.8.8-R0.1-SNAPSHOT.jar"))
    compileOnly("org.jetbrains:annotations:24.0.1")
    // Provides CraftBukkit/NMS classes for v1_8_R3 compilation.
    compileOnly(files("lib/spigot/1.8.8-R0.1.jar"))

    // NMS 1.8.8 sources reference Guava Predicate.
    compileOnly("com.google.guava:guava:17.0")

    // Citizens API (runtime plugin dependency, not shaded).
    compileOnly(files("lib/Citizens-2.0.30-b2803.jar"))
}

// 1.8.8-only build: exclude 1.9+ implementation sources.
sourceSets {
    named("main") {
        java {
            exclude(
                "**/creatures/v1_9_UP/**",
                "**/arena/managers/enemy/spawner/EnemySpawnerRegistry.java",
                "**/arena/managers/CreatureTargetManager.java"
            )
        }
    }
}

configurations {
    // Exclude bungeecord-chat dependency from all configurations to avoid remote snapshot resolution.
    all {
        exclude(group = "net.md-5", module = "bungeecord-chat")
    }
}

group = "plugily.projects"
version = "4.7.0"
description = "VillageDefense"
java {
    withJavadocJar()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile> {
    // Windows default encoding may be GBK; force UTF-8 for Java sources/resources.
    options.encoding = "UTF-8"
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("plugily.projects.minigamesbox", "plugily.projects.villagedefense.minigamesbox")
        relocate("com.zaxxer.hikari", "plugily.projects.villagedefense.database.hikari")
        minimize()
    }

    processResources {
        filesMatching("**/plugin.yml") {
            expand(project.properties)
        }
    }

    javadoc {
        options.encoding = "UTF-8"
    }

}

publishing {
    repositories {
        maven {
            name = "Releases"
            url = uri("https://maven.plugily.xyz/releases")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
        maven {
            name = "Snapshots"
            url = uri("https://maven.plugily.xyz/snapshots")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
