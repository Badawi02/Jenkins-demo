FROM jenkins/inbound-agent:latest-jdk17

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven git bash curl ca-certificates \
    && git config --system --add safe.directory '*' \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
