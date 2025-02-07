ARG PYTHON_VERSION=3.11

FROM mcr.microsoft.com/devcontainers/python:${PYTHON_VERSION}-bullseye

ARG PYTHON_PROJECT_FOLDER="metorikku"
ARG FURY_URL="https://pypi.fury.io/syngenta-digital/"
ARG FURY_AUTH
ARG POETRY_VERSION=1.6.1
ARG USERNAME=vscode

ENV DEBIAN_FRONTEND=noninteractive
ENV AWS_REGION=eu-central-1
ENV PATH "/home/${USERNAME}/.local/bin:$PATH"

# Install base packages
RUN apt-get update \
    && apt-get -y install --no-install-recommends apt-utils 2>&1 \
    && apt-get -y install git procps lsb-release unzip curl wget gnupg2 vim python3 python3-dev python3-distutils default-jdk icu-devtools

# Install awscli
RUN mkdir -p /tmp/docker-downloads \
    && curl "https://awscli.amazonaws.com/awscli-exe-linux-$(arch).zip" -o "/tmp/docker-downloads/awscliv2.zip" \
    && cd /tmp/docker-downloads \
    && unzip awscliv2.zip \
    && ./aws/install

USER $USERNAME
WORKDIR /home/${USERNAME}/

SHELL ["/bin/bash", "-c"]

ADD .devcontainer/init_container.sh /usr/local/bin/
ADD .devcontainer/hive-site.xml "/tmp/"

# Install Java & related
RUN curl -s "https://get.sdkman.io?rcupdate=false" | bash

ENV JAVA_VERSION="17.0.13-amzn"
ENV SCALA_VERSION="2.12.18"
ENV SCALA_BINARY_VERSION="2.12"
ENV SBT_VERSION="1.10.6"
ENV SPARK_VERSION="3.5.2"

RUN source "${HOME}/.sdkman/bin/sdkman-init.sh" \
    && sdk install java "$JAVA_VERSION" \
    && sdk install scala "$SCALA_VERSION" \
    && sdk install sbt "$SBT_VERSION" \
    && sdk install spark "$SPARK_VERSION" \
    && rm -Rf "${HOME}/.sdkman/candidates/spark/current"/jars/guava-*.jar \
    && cp /tmp/hive-site.xml "${HOME}/.sdkman/candidates/spark/current/conf/"

# Clean up
ENV DEBIAN_FRONTEND=dialog

USER root
RUN cd \
    && rm -rf /tmp/docker-downloads \
    && apt-get autoremove -y \
    && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/* \
    && rm -Rf /tmp/project/

# Configure profile
USER $USERNAME

RUN echo '' >> "${HOME}/.bashrc"
RUN echo 'export PATH="${PATH}":${HOME}/.local/bin' >> "${HOME}/.bashrc"
RUN echo 'export AWS_REGION=eu-central-1' >> "${HOME}/.bashrc"
RUN echo '[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"' >> "${HOME}/.bashrc"
RUN echo 'export SBT_OPTS="-Xms512M -Xmx2G -Xss2M -XX:MaxMetaspaceSize=1G"' >> "${HOME}/.bashrc"
