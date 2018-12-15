# Java Plugin

## JDK compatibility

```groovy
sourceCompatibility = '11'
targetCompatibility = '11'
```

## Build jar file

Set group and version:

```groovy
group = 'alvin.gradle'
version = "${System.getenv('CI_VERSION') ?: 1}.${System.getenv('CI_BUILD_NUM') ?: 0}"
```

This demo with the following dependencies:

```groovy
dependencies {
    implementation "com.google.guava:guava:${versions.guava}"
}
```

### Shadow Jar 

- First, copy all dependency jar files into a dir:

    ```groovy
    task copyJarsToLib {
        def toDir = "${buildDir}/libs/libs"
        doLast {
            copy {
                from configurations.compileClasspath
                into toDir
            }
        }
    }
    ```
- Get `classpath` of all dependency jar files:
    ```groovy
    def makeClassPath() {
        def jarNames = configurations.compileClasspath.collect {
            it.getName()
        }
        return '. libs/' + jarNames.join(' libs/')
    }
    ```
- Create target jar

    ```groovy
    jar {
        manifest {
            attributes(
                'Implementation-Title': 'Gradle Demo',
                'Implementation-Version': version,
                'Main-Class': 'alvin.gradle.java.Main',
                "Class-Path": makeClassPath()
            )
        }
        archiveName "${project.group}.${project.name}-${project.version}.jar"
        from "${buildDir}/libs/libs"
    }
    ```
    
Finally, the target files in build/libs are:
```
.
├── alvin.gradle.java-1.0.jar
└── libs
    ├── guava-27.0.1-jre.jar
    ├── jsr305-3.0.2.jar
    └── ...
```

### Fat jar

Compile `.class` file of current project and all `.class` file in dependency jar files together in one `.jar` file:

```groovy
task fatJar(type: Jar) {
    exclude 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA', 'META-INF/*.MF'

    manifest {
        attributes(
            'Implementation-Title': 'Gradle Demo',
            'Implementation-Version': version,
            'Main-Class': 'alvin.gradle.java.Main',
        )
    }

    archiveName "${project.group}.${project.name}-${project.version}-fat.jar"
    
    from {
        configurations.compileClasspath.collect {
            it.isDirectory() ? it : zipTree(it)
        }
    }
    with jar
}
```

In target `.jar` file:

```
alvin.gradle.java-1.0-fat.jar
|
├── META-INF
│   ├── MANIFEST.MF
│   └── maven
│       ├── com.google.code.findbugs
│       │   └── jsr305
│       │       ├── pom.properties
│       │       └── pom.xml
│       └── ...
├── alvin
│   └── gradle
│       └── java
│           └── Main.class
├── com
│   └── google
│       ├── common
│       │   └── ...
│       └── ...
├── javax
│   └── annotation
│       └── ...
└── ...
```
