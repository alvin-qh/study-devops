# Jacoco plugin

## Setup

### Install

Edit `build.gradle`, add jacoco plugin and config

```groovy
plugins {
    // ...
    id 'jacoco'
}
```

```groovy
jacoco {
    toolVersion = "0.8.2"
    // directory to output reports
    reportsDir = file("$buildDir/reports/jacoco")
}
```

### Generate a report

Edit `build.gradle`, and config `jacocoTestReport` task:

```groovy
jacocoTestReport {
    reports {
        xml.enabled true
        html.enabled true
        csv.enabled false
//      html.destination file("${buildDir}/test-report")
    }
    afterEvaluate {
        classDirectories.from = files(classDirectories.files.collect {
            fileTree(dir: it, exclude: ['resources/**'])
        })
    }
}
```

> Make sure xml report should be created if coverage is needed


## Calculate coverage

Add `thresholds.xml` in project dir

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<thresholds>
    <coverage>
        <line>0.8</line>
        <branch>0.8</branch>
    </coverage>
</thresholds>
```

Edit `build.gradle`, add new task to calculate coverage

```groovy
task coverage(dependsOn: jacocoTestReport) {
    doLast {
        // load thresholds.xml file
        def thresholds = parseXml("${projectDir}/thresholds.xml").coverage[0]
        // load jacocoTestReport.xml file
        def report = parseXml("${buildDir}/reports/jacoco/test/jacocoTestReport.xml")

        failIfBelowThreshold(thresholds, report, 'line')
        failIfBelowThreshold(thresholds, report, 'branch')
    }
}

coverage.shouldRunAfter test
```

What is `failIfBelowThreshold` method do?

- Load `@covered ÷ (@covered + @missed)` from `:report > :counter[@type = LINE]` in `jacocoTestReport.xml`
- Load `@covered ÷ (@covered + @missed)` from `:report > :counter[@type = BRANCH]` in `jacocoTestReport.xml`
- Load expect value from `:thresholds > :coverage > :line > :text` in `thresholds.xml`
- Load expect value from `:thresholds > :coverage > :branch > :text` in `thresholds.xml`
- Compare those values
