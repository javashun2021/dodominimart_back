// DODOMINIMART 后台自动化部署流水线
// Jenkins 与 app 同机(生产服务器 8.217.186.177),部署 = 本地 cp jar + 跑 /app/ruoyi/start.sh
// 触发:GitHub push 到 master → webhook → 本流水线
pipeline {
    agent any

    // 需在 Jenkins「全局工具配置」里配好同名的 Maven 与 JDK(见文档说明)
    tools {
        maven 'M3'
        jdk   'JDK8'
    }

    options {
        timestamps()
        disableConcurrentBuilds()               // 同一时间只跑一次部署,避免 jar 被并发覆盖
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        APP_DIR    = '/app/ruoyi'
        JAR_SRC    = 'ruoyi-admin/target/ruoyi-admin.jar'
        MAVEN_OPTS = '-Xmx512m'                  // 2G 单机:限制 Maven 堆,给运行中的 app 留内存
        HEALTH_URL = 'http://127.0.0.1:8080/login'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // 只打 admin 及其依赖模块,跳过测试
                sh 'mvn -B clean package -DskipTests -pl ruoyi-admin -am'
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    test -f "$JAR_SRC" || { echo "jar not found: $JAR_SRC"; exit 1; }

                    # 备份当前 jar,便于回滚(保留最近若干份)
                    mkdir -p "$APP_DIR/backup"
                    if [ -f "$APP_DIR/ruoyi-admin.jar" ]; then
                        cp "$APP_DIR/ruoyi-admin.jar" "$APP_DIR/backup/ruoyi-admin.$(date +%Y%m%d_%H%M%S).jar"
                        # 只保留最近 5 个备份
                        ls -1t "$APP_DIR"/backup/ruoyi-admin.*.jar | tail -n +6 | xargs -r rm -f
                    fi

                    cp "$JAR_SRC" "$APP_DIR/ruoyi-admin.jar"
                    bash "$APP_DIR/start.sh"
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    set +e
                    for i in $(seq 1 40); do
                        code=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL")
                        if [ "$code" = "200" ]; then
                            echo "App is UP (HTTP 200)"; exit 0
                        fi
                        echo "waiting for app... ($i) http=$code"
                        sleep 3
                    done
                    echo "App failed health check in time. Recent log:"
                    tail -n 60 "$APP_DIR/logs/app.log" || true
                    exit 1
                '''
            }
        }
    }

    post {
        success { echo "✅ Deployed. Build #${env.BUILD_NUMBER}" }
        failure { echo "❌ Build/deploy FAILED. Check console + $APP_DIR/logs/app.log" }
    }
}
