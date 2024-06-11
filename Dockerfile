# https://github.com/exo-docker/jdk/openjdk-17/ubuntu/24.04/Dockerfile
# Dockerizing a base images with:
#
#   - Ubuntu 24.04 LTS (Noble Numbat)
#   - OpenJDK 17
#
# Build:    docker build -t exoplatform/jdk:openjdk-17 .
#
# Run:      docker run -ti exoplatform/jdk:openjdk-17 -version

# 使用 Ubuntu 24.04 LTS (Noble Numbat) 作为基础镜像
FROM exoplatform/ubuntu:24.04
LABEL maintainer="eXo Platform <docker@exoplatform.com>"

ENV JDK_MAJOR_VERSION=17

# 更新软件包并安装依赖
RUN apt-get update -qq && \
    apt-get install -qq -y gnupg ca-certificates curl

# 添加 Azul Zulu 的 GPG 密钥和软件源
RUN curl -s https://repos.azul.com/azul-repo.key | gpg --dearmor -o /usr/share/keyrings/azul.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | tee /etc/apt/sources.list.d/zulu.list

# 更新软件包并安装 OpenJDK 17
RUN apt-get update -qq && \
    apt-get install -qq -y zulu${JDK_MAJOR_VERSION}-jdk tini && \
    apt-get autoremove -qq -y && \
    apt-get clean -qq -y && \
    rm -rf /var/lib/apt/lists/*

# 设置 JAVA_HOME 环境变量
ENV JAVA_HOME /usr/lib/jvm/zulu${JDK_MAJOR_VERSION}-ca-amd64

# 设置入口点
ENTRYPOINT ["/usr/local/bin/tini", "--", "/usr/bin/java"]