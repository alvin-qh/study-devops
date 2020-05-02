# Gradle

## 1. Setup environment

### 1.1. Install gradle

#### 1.1.1. Install java

Install open jdk

```bash
$ sudo apt install openjdk-11-jdk-headless
```

Or following the [Offical document](https://openjdk.java.net/install/)

#### 1.1.2. Set default jdk

First, you need to run the following commands to add JDK 8 to the alternatives system.

```bash
$ sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-11-openjdk-amd64/bin/java 1

$ sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-11-openjdk-amd64/bin/javac 1
```

To set a default JDK on Ubuntu 18.04/19.04, run the following command:

```bash
$ sudo update-alternatives --config java

$ sudo update-alternatives --config javac
```

Use `java -version` and `javac -version` command to check the default JDK

### 1.2. Install gradle

#### 1.2.1. Download gradle

Check [newest version of gradle](https://gradle.org/releases/)

Download gradle 

```bash
$ wget https://services.gradle.org/distributions/gradle-6.3-bin.zip

$ unzip gradle-6.3-bin.zip
```

#### 1.2.2. Setup gradle

Edit `~/.zshrc` (or `~/.bash_profile`, or `~/.bashrc`) file, add following content into it:

```bash
GRADLE_HOME=/usr/lib/gradle-6.3
PATH=$GRADLE_HOME/bin:$PATH
export PATH GRADLE_HOME
```

### 1.3. Install python

#### 1.3.1. Install pyenv

- Download and install pyenv

```bash
$ curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
```

- Set shell enviroment: modify `~/.bashrc` (or `~/.zshrc` or `~/.bash_profile`), and add the following content

```bash
export PATH="~/.pyenv/bin:$PATH"
eval "$(pyenv init -)"
eval "$(pyenv virtualenv-init -)"
```

#### 1.3.2. Install python

```bash
$ pyenv install 3.7.5
```

#### 1.3.3. Use python

In notebook folder: 

```bash
$ pyenv local 3.7.5 
```

## 2. Setup notebook

[Use jupyter notebook](./notebook/README.md)