# 项目组

- [项目组](#项目组)
  - [1. 项目组结构](#1-项目组结构)
  - [2. 定义项目组](#2-定义项目组)
    - [2.1. 定义模块](#21-定义模块)
    - [2.2. 定义依赖管理](#22-定义依赖管理)
    - [2.3. 定义构建插件管理](#23-定义构建插件管理)
    - [2.3. 从根项目继承](#23-从根项目继承)
    - [2.4. 项目组项目相互引用](#24-项目组项目相互引用)

## 1. 项目组结构

大部分时候，一个工程应该由多个独立的项目组成，例如将公共的部分放在 "library" 项目中

Maven 可以创建包含多个子项目的工程，通过父子关系组织它们的结构，例如：

```plain
project
 ├─ pom.xml
 ├─ app
 │  ├─ pom.xml
 │  └─ src
 │     ├─ main
 │     │  └─ java
 │     │     └─ App.java
 │     └─ test
 │        └─ java
 │           └─ AppTest.java
 └─ lib
    ├─ pom.xml
    └─ src
       ├─ main
       │  └─ java
       │     └─ Lib.java
       └─ test
          └─ java
             └─ LibTest.java
```

在同一个项目组中，一个项目可以通过将另一个项目作为当前项目**依赖**的方式，引用另一个项目中的**类**

## 2. 定义项目组

在整个工程的根路径下面，需要定义根 `pom.xml`，除此之外，每个子项目下，也要定义 `pom.xml`，并通过父子关系将他们关联起来

### 2.1. 定义模块

在整个工程的根 `pom.xml` 中，需要定义 `module` 标签，来指定要包含模块（子项目）

包含路径 `./app` 和 `./lib` 下的模块

```xml
<modules>
    <module>app</module>
    <module>lib</module>
</modules>
```

根项目的 `<packaging>` 标签必须定义为 `pom`，表示这是一个 BOM，会包含子模块

### 2.2. 定义依赖管理

可以通过 `dependencyManagement` 标签在根项目中对子项目使用的依赖做规范性管理，保证所有子项目使用依赖的版本

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>${version.junit}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${version.guava}</version>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${version.lambok}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

如此一来即可定义指定的诸多依赖及其以来版本。这些依赖定义并不会将依赖引入项目中，但可以保证所有引入这些依赖的地方都是一致的

### 2.3. 定义构建插件管理

构建是通过 `<build>` 标签下的一系列构建插件完成的，可以通过 `<pluginManagement>` 标签管理构建插件的定义

```xml
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${version.maven-compiler}</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>

            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${version.maven-surefire}</version>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>${version.maven-exec}</version>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>${version.maven-jar}</version>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>${version.maven-shade}</version>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

如此一来即可定义指定的诸多构建查查。这些插件定义并不会令这些插件起作用，但可以保证使用插件的地方保持一致

### 2.3. 从根项目继承

定义的子模块（子项目）可以通过 `<parent>` 标签继承根项目（当然，也可以不继承，或从另一个 `pom` 定义继承）

继承会从父 `pom` 中继承除项目定义外的其它内容，包括 `<dependencyManagement>`，`<dependencies>`，`<build>`，`<reporting>` 等内容。继承方无需再重复定义

```xml
<parent>
    <groupId>alvin.study</groupId>
    <artifactId>study-maven-subprojects</artifactId>
    <version>1.0-SNAPSHOT</version>
    <relativePath>..</relativePath>
</parent>
```

继承要指明被继承 `pom.xml` 的 `groupId`，`artifactId` 和 `version`。如果继承的 `pom.xml` 来自本地，可以通过 `<relativePath>` 标签指定其相对路径，加快构建速度

### 2.4. 项目组项目相互引用

可以将项目组的某个项目作为依赖在当前项目引用，引用方式和引用 maven 其它依赖类似

在根 `pom.xml` 中定义依赖管理，定义对某个子模块的依赖，当然也可以不定义，但这样就无法对依赖版本做统一管理

```xml
<dependencyManagement>
    <dependencies>
        <groupId>${project.groupId}</groupId>
        <artifactId>study-maven-subprojects-lib</artifactId>
        <version>${project.version}</version>
    </dependencies>
</dependencyManagement>
```

表示将当前 `groupId` 下 `artifactId` 为 `study-maven-subprojects-lib` 定义为依赖

在子模块 `pom.xml` 中，即可引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>study-maven-subprojects-lib</artifactId>
    </dependency>
</dependencies>
```

这样就可以使用 `study-maven-subprojects-lib` 项目中定义的类
