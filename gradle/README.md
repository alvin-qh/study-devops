# Gradle projects

## Plugins

### Use plugin

```groovy
apply plugin: 'id of plugin'
```
or
```groovy
plugins {
    id 'id of plugin'
    id 'id of plugin'
}
```

### Plugin repositories

```groovy
buildscript {
    repositories {
        jcenter()
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
}
```

## Modularization

Include other `.gradle` file

```groovy
apply from: 'depends.gradle'
```

## IDEA Plugin

The `idea plugin` bring the `idea` and `cleanIdea` tasks
- `idea` task can create `project.ipr`, can open in IDE directly
- `cleanIdea` task can clean the effects what `idea` task was banged

### In root build.gradle file

```groovy
apply plugin: 'idea'

idea {
    project {
        jdkName = '11'
        languageLevel = '11'
    }
}
```

### In every sub-project build.gradle file

```groovy
plugins {
    // ...
    id 'idea'
}
```

## Unit test with JUnit 5

```groovy
test {
   useJUnitPlatform()
}
```
