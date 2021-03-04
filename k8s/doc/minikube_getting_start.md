# Minikube getting started

## 1. 安装K8S

### 1.1. For Debian

- 安装依赖

```bash
$ sudo apt update
$ sudo apt install -y apt-transport-https
```

- 安装许可证key

```bash
$ curl https://mirrors.aliyun.com/kubernetes/apt/doc/apt-key.gpg | \
      sudo apt-key add -
```

- 设置安装源（国内源）

```Bash
cat <<EOF >/etc/apt/sources.list.d/kubernetes.list
deb https://mirrors.aliyun.com/kubernetes/apt/ kubernetes-xenial main
EOF
```

- 安装k8s

```bash
$ sudo apt update
$ sudo apt install -y kubelet kubeadm kubectl
```

### 1.2. For centOS

- 设置软件源（国内源）

```Bash
$ cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg https://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
EOF
```

- 安装k8s

```Bash
$ sudo yum install -y kubelet kubeadm kubectl
```

### 1.3. 设置命令补全

- 安装依赖（for centos）

```Bash
$ sudo yum install bash-completion -y
```

- 安装依赖（for debian）

```Bash
$ sudo apt install bash-completion -y
```

- 设置命令补全

```Bash
$ echo "source <(kubectl completion bash)" >> ~/.bashrc
```

#### 1.4. 关闭防火墙和交换区

- 关闭防火墙（ufw）

```bash
# 查看防火墙状态
$ sudo ufw status

# 如果防火墙安装且打开，则将其关闭
$ sudo ufw disable
```

- 关闭防火墙（iptables）

```bash
# 查看防火墙状态
$ sudo iptables -L

# 临时关闭防火墙
$ sudo systemctl stop iptables

# 永久关闭防火墙
$ sudo chkconfig iptables off
$ sudo chkconfig --del iptables
```

- 关闭交换区

```bash
# 关闭交换区
$ sudo sysctl -w vm.swappiness=0

# 永久关闭交换区
$ sudo su -
$ echo "vm.swappiness = 0">> /etc/sysctl.conf
$ sed -ri 's/.*swap.*/#&/' /etc/fstab

# 生效
$ swapoff -a
$ sysctl -p
$ exit
```

## 2. 安装minikube

> See also：[https://minikube.sigs.k8s.io/docs/start/](https://minikube.sigs.k8s.io/docs/start/)

### 2.1. 下载minikube

```bash
$ curl -Lo minikube \
    https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64 \
    && chmod +x minikube

$ sudo mv ./minikube /usr/local/bin/
```

### 2.2. 安装制定的hypervisor

#### 2.2.1. 安装docker

安装docker

#### 2.2.2. 安装virtualbox

- 安装依赖

```bash
$ sudo apt update
$ sudo apt install -y apt-transport-https
```

- 安装virtualbox（以virtualbox为hypervisor）

```bash
$ sudo apt install -y virtualbox virtualbox-ext-pack
```

#### 2.2.3. 设置默认hypervisor driver

```bash
$ minikube config set driver docker
```

> 或在启动时指定driver：`$ minikube start --driver=docker`

See Also：



### 2.3. 启动minikube

```bash
$ minikube start \
    --image-mirror-country='cn' \
    --image-repository='registry.cn-hangzhou.aliyuncs.com/google_containers'
```

> 使用`--image-mirror-country`和`--image-repository`两个参数指定国内源下载镜像

### 2.4. Troubleshooting

#### 2.4.1. Verify Docker container type is Linux

On Windows, make sure Docker Desktop’s container type setting is Linux and not windows. see docker docs on [switching container type](https://docs.docker.com/docker-for-windows/#switch-between-windows-and-linux-containers). You can verify your Docker container type by running:

```Bash
$ docker info --format '{{.OSType}}'

```

#### 2.4.2. Run with logs

执行`minikube`命令时，使用`--alsologtostderr -v=1`参数，可以输出详细的log，用于问题梳理和调试

## 3. 与集群交互

#### 3.1. 访问集群

- 获取集群服务列表

```Bash
$ kubectl get po -A
```

- 也可以下载适当版本的`kubectl`

```Bash
minikube kubectl -- get po -A
```

#### 3.2. 启动Kubernets仪表盘

- 启动仪表盘

```Bash
$ minikube dashboard --url
```

- 如果在虚拟机部署，则可通过宿主机的ssh进行端口映射

```Bash
$ ssh -qTfnN -L <local_port>:localhost:<remote_port> <name>@<vm_ip>
```

- 使仪表盘可以远程访问

```Bash
$ kubectl proxy --port=33458 --address='0.0.0.0' --accept-hosts='^.*' &
```

## 4. 部署应用

#### 4.1. 部署简单的echoserver进行测试

- 创建一个名为`hello-minikube`的deployment

```Bash
$ kubectl create deployment hello-minikube \
    --image=registry.aliyuncs.com/google_containers/echoserver:1.4
```

  部署选用`registry.aliyuncs.com/google_containers/echoserver:1.4`作为镜像，该镜像

  此时会创建一个**pod**，用于

- 导出该deployment，并在8080端口上暴露服务

```bash
$ kubectl expose deployment hello-minikube --type=NodePort --port=8080
```

- 此时，可以查看服务是否已经存在

```bash
$ kubectl get services hello-minikube
```

- 可以使用minikube命令快速获取服务访问地址

```bash
$ minikube service hello-minikube

|-----------|----------------|-------------|---------------------------|
| NAMESPACE |      NAME      | TARGET PORT |            URL            |
|-----------|----------------|-------------|---------------------------|
| default   | hello-minikube |        8080 | http://192.168.49.2:30049 |
|-----------|----------------|-------------|---------------------------|
🎉  Opening service default/hello-minikube in default browser...
👉  http://192.168.49.2:30049
```

  > 地址http://192.168.49.2:30049是服务`hello-minikube`在容器中的访问地址，该地址无法从宿主机以外直接访问
可以通过`ssh -D localhost:10080 <name>@<host>`设置代理，访问目标地址

- 创建端口映射

```bash
$ kubectl port-forward service/hello-minikube 7080:8080
```

  > 此时，宿主机的`7080`端口会映射到容器的`8080`端口，可通过`localhost:7080`访问服务



```Bash
$ minikube service hello-minikube --url
```

