# Jenkins 自动化部署（DODOMINIMART 后台）

目标：`git push` 到 GitHub master → Jenkins 自动 **拉代码 → Maven 打包 → cp jar → 重启 → 健康检查**。
Jenkins 与 app 同机（生产服务器 `8.217.186.177`），部署是本地操作，无需 SSH/scp。

流水线定义已入库：仓库根目录 **`Jenkinsfile`**。你只需在服务器把 Jenkins 装好、配好任务，之后一切自动。

---

## ⚠️ 前置 0：加 Swap（务必先做）

这台 2G 机器上同时跑着 **MySQL + app（-Xmx512m）**，再加 Jenkins + Maven 构建，极易 OOM。
先加 2G swap 兜底：

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h    # 确认 Swap 那行有 2G
```

---

## 1. 安装 Jenkins

Jenkins（近年 LTS）**运行需要 JDK 17**，但**构建 app 仍用 JDK 8**。两者共存：JDK17 给 Jenkins 自己跑，JDK8 在 Jenkins 里注册给构建用。

```bash
# 安装 JDK17（供 Jenkins 运行）
sudo apt-get update
sudo apt-get install -y fontconfig openjdk-17-jre

# 安装 Jenkins LTS（Debian/Ubuntu 源）
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/" | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo apt-get install -y jenkins
```
> CentOS/Alibaba Linux 用 yum 源，参考 Jenkins 官网 RedHat 安装章节。

### 改端口为 8081（app 占用 8080）

```bash
sudo systemctl edit jenkins
```
写入：
```ini
[Service]
Environment="JENKINS_PORT=8081"
```
然后：
```bash
sudo systemctl restart jenkins
sudo systemctl enable jenkins
```
访问 `http://8.217.186.177:8081`，初始密码：
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```
装「Suggested plugins」，另确认装上 **Git**、**Pipeline**、**GitHub** 插件。

### 云安全组放行

在阿里云/服务商控制台**安全组**放行入方向 **TCP 8081**（Jenkins 面板 + GitHub webhook 回调都走它）。

---

## 2. 让 Jenkins 有权部署

Jenkins 以 `jenkins` 用户运行，需能写 `/app/ruoyi` 和上传目录，并能重启 app：

```bash
sudo chown -R jenkins:jenkins /app/ruoyi
sudo mkdir -p /home/dodominimart/uploadPath
sudo chown -R jenkins:jenkins /home/dodominimart
```
> 注意：这样重启后 app 会以 `jenkins` 用户身份运行。确认它对 `/app/ruoyi/config`、日志、上传目录都可读写（上面 chown 已覆盖）。

把加固版启动脚本放到服务器（本仓库 `deploy/start.sh` 是最新版，`deploy/` 被 gitignore，需手动上传一次）：
```bash
# 从本地上传，或直接在服务器编辑 /app/ruoyi/start.sh 用最新内容
chmod +x /app/ruoyi/start.sh
```

---

## 3. Jenkins 全局工具配置

「Manage Jenkins → Tools」：

- **JDK**：名字填 `JDK8`（要和 Jenkinsfile 里的 `jdk 'JDK8'` 一致）。
  - 关掉「Install automatically」，`JAVA_HOME` 填服务器上 JDK8 路径（如 `/usr/lib/jvm/java-8-openjdk-amd64`）。没装就 `sudo apt-get install -y openjdk-8-jdk`。
- **Maven**：名字填 `M3`（和 Jenkinsfile 里 `maven 'M3'` 一致）。勾「Install automatically」选 3.6.3 即可，或指向服务器已装的 Maven。

---

## 4. GitHub 凭据（私有仓库）

仓库 `dodominimart_back` 是私有的，Jenkins 需要凭据拉代码：

1. GitHub → Settings → Developer settings → **Personal access token (classic)**，勾 `repo`，生成 token。
2. Jenkins →「Manage Jenkins → Credentials → System → Global → Add Credentials」：
   - Kind: **Username with password**
   - Username: 你的 GitHub 用户名 `javashun2021`
   - Password: 刚生成的 token
   - ID: 填 `github-cred`（方便记）

---

## 5. 创建 Pipeline 任务

「New Item」→ 名称 `dodominimart-deploy` → 选 **Pipeline** → OK。

- **Build Triggers**：勾 ✅ **GitHub hook trigger for GITScm polling**
- **Pipeline** 区：
  - Definition: **Pipeline script from SCM**
  - SCM: **Git**
  - Repository URL: `https://github.com/javashun2021/dodominimart_back.git`
  - Credentials: 选 `github-cred`
  - Branch: `*/master`
  - Script Path: `Jenkinsfile`
- 保存。

先点一次 **Build Now** 手动验证整条链路能跑通（拉码→打包→部署→健康检查绿灯）。

---

## 6. 配 GitHub Webhook（实现 push 自动部署）

GitHub 仓库 → Settings → **Webhooks → Add webhook**：

- Payload URL：`http://8.217.186.177:8081/github-webhook/`（**结尾斜杠不能少**）
- Content type：`application/json`
- 事件：Just the push event
- Add webhook

之后每次 `git push origin master`，GitHub 会回调 Jenkins，自动触发本流水线。
（Recent Deliveries 里能看到回调是否 200 成功。）

---

## 流水线做了什么（Jenkinsfile）

1. **Checkout** 从 master 拉最新代码
2. **Build** `mvn clean package -DskipTests -pl ruoyi-admin -am`（`MAVEN_OPTS=-Xmx512m` 限堆）
3. **Deploy** 备份旧 jar（保留最近 5 份，在 `/app/ruoyi/backup/`）→ cp 新 jar → 跑 `start.sh` 重启
4. **Health Check** 轮询 `http://127.0.0.1:8080/login` 直到 HTTP 200；超时则打印日志尾部并失败

## 回滚

部署失败或上线后发现问题，手动回滚到上一份 jar：
```bash
ls -1t /app/ruoyi/backup/          # 找到要回滚的备份
cp /app/ruoyi/backup/ruoyi-admin.<时间戳>.jar /app/ruoyi/ruoyi-admin.jar
bash /app/ruoyi/start.sh
```

## 常见问题

- **构建时 OOM / 机器卡死**：确认已加 swap（前置 0）；必要时把 app 的 `-Xmx512m` 或 Jenkins 的堆调小。
- **health check 一直不过**：`tail -f /app/ruoyi/logs/app.log` 看真实报错（多为 DB 连接、端口占用）。
- **8080 端口被占**：加固版 `start.sh` 已做「等旧进程退出 + 超时强杀」，若仍占用，`lsof -i:8080` 查残留进程。
- **webhook 不触发**：查 GitHub webhook 的 Recent Deliveries 响应码；确认安全组放行 8081、Jenkins 任务勾了 GitHub hook trigger。
