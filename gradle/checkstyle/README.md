# Gradle checkstyle plugin

## Setup

### Install plugin

Edit `build.gradle` file, add plugin define:

```groovy
plugins {
    // ...
    id 'checkstyle'
}

// ...
checkstyle {
    toolVersion = '8.15'    // specify the version
    showViolations true
}
```

### Specify the configuration XML file

Edit `build.gradle` file, add tasks define:

```groovy
checkstyleMain {
    outputs.upToDateWhen { false }
    configFile = file("${project.projectDir}/config/checkstyle.xml")
}

checkstyleTest {
    outputs.upToDateWhen { false }
    configFile = file("${project.projectDir}/config/checkstyle-test.xml")
}
```

> `output.upToDateWhen { false }` means this task must be ran every time, and never be up-to-date

### Interrupt the build process when check failed

Edit `./config/checkstyle.xml` (or `./config/checkstyle-test.xml`), add the following element in `Checker` node:

```xml
<property name="severity" value="error"/>
```
