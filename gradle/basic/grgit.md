# Use grgit plugin


## Install grgit plugin 


### 1. Add plugin dependency repository
 
Add plugin dependency repository in `build.gradle` at root project dir

```groovy
buildscript {
    ext {
       versions = [
           // ...
           grgit: '3.0.0'
       ]
    }
    
    repositors {
       jcenter()
       // ...
    }
    
    dependencies {
       classpath "org.ajoberstar.grgit:grgit-gradle:${versions.grgit}"
    }
}
```

### 2. Use plugin

In `build.gradle`, add plugin define:

```groovy
plugins {
    // ...
    id 'org.ajoberstar.grgit'
}
```

## Document

Visit [Project home](http://ajoberstar.org/grgit/index.html)

