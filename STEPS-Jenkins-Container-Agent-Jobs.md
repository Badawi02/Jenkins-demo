# Jenkins Container Demo Steps

This file contains a complete instructor demo for students.

The demo covers:

1. Install Jenkins as a normal Docker container, without Docker Compose.
2. Configure Jenkins with a new slave/agent.
3. Start the slave/agent as another Docker container.
4. Build a simple Java Maven app using three job types:
   - Freestyle
   - Pipeline
   - Multibranch Pipeline

---

## Demo Folder Structure

```text
jenkins-manual-container-demo/
├── STEPS-Jenkins-Container-Agent-Jobs.md
├── agent/
│   └── Dockerfile
└── java-demo-app/
    ├── pom.xml
    ├── Jenkinsfile
    ├── Jenkinsfile-feature
    └── src/
```

---

## Prerequisites

On the demo machine, install:

- Docker
- Git
- curl

Check:

```bash
docker --version
git --version
curl --version
```

---

# Part 1 — Install Jenkins as Normal Container

Go to the demo folder:

```bash
cd jenkins-manual-container-demo
```

Save the current path:

```bash
export DEMO_DIR="$(pwd)"
echo $DEMO_DIR
```

Create Docker network:

```bash
docker network create jenkins-net
```

Create Jenkins home volume:

```bash
docker volume create jenkins_home
```

Run Jenkins controller container:

```bash
docker run -d \
  --name jenkins-master \
  --network jenkins-net \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v "$DEMO_DIR/java-demo-app:$DEMO_DIR/java-demo-app" \
  jenkins/jenkins:lts-jdk17
```

Why we mounted `java-demo-app`:

- Multibranch Pipeline needs Git repository access.
- Jenkins controller and agent containers both need to see the same local repo path.
- In real projects, you normally use GitLab/GitHub URL instead.

Check Jenkins container:

```bash
docker ps
```

Install Git inside the Jenkins controller container:

```bash
docker exec -u root jenkins-master bash -lc "apt-get update && apt-get install -y git && git config --system --add safe.directory '*' && rm -rf /var/lib/apt/lists/*"
```

Why this is needed:

- The Multibranch Pipeline scan happens from Jenkins controller.
- The controller may need Git to discover branches.
- `safe.directory '*'` avoids Git ownership warnings inside containers during the classroom demo.

Get initial admin password:

```bash
docker exec jenkins-master cat /var/jenkins_home/secrets/initialAdminPassword
```

Open Jenkins:

```text
http://localhost:8080
```

Initial setup:

1. Paste the initial admin password.
2. Choose `Install suggested plugins`.
3. Create admin user.

Recommended demo user:

```text
Username: admin
Password: admin123
```

---

# Part 2 — Prepare the Simple Java App Git Repository

From the demo folder:

```bash
cd "$DEMO_DIR/java-demo-app"
git init
git config user.email "student@example.com"
git config user.name "Student"
git add .
git commit -m "Initial Java Maven app"
git branch -M main
```

Create a feature branch for the Multibranch Pipeline demo:

```bash
git checkout -b feature/demo-change
cp Jenkinsfile-feature Jenkinsfile
git add Jenkinsfile
git commit -m "Feature branch Jenkinsfile"
git checkout main
```

Test locally if Maven is installed on your host:

```bash
mvn clean test
```

If Maven is not installed on the host, no problem. Jenkins agent container will have Maven.

---

# Part 3 — Configure Jenkins with New Slave/Agent

In Jenkins UI:

```text
Manage Jenkins → Nodes → New Node
```

Use:

```text
Node name: docker-slave-1
Type: Permanent Agent
```

Click `Create`.

Configure the node:

```text
Description: Docker container slave for Java Maven builds
Number of executors: 1
Remote root directory: /home/jenkins/agent
Labels: docker linux maven slave
Usage: Only build jobs with label expressions matching this node
Launch method: Launch agent by connecting it to the controller
```

Click `Save`.

Open the node page:

```text
Manage Jenkins → Nodes → docker-slave-1
```

You will see a command similar to:

```bash
java -jar agent.jar -url http://localhost:8080/ -secret xxxxxxxxx -name docker-slave-1 -webSocket -workDir "/home/jenkins/agent"
```

Copy the secret value after `-secret`.

Example:

```text
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

---

# Part 4 — Start New Slave as Container

First build the custom Jenkins agent image.

This image includes:

- Java 17
- Maven
- Git
- bash

Run:

```bash
cd "$DEMO_DIR"
docker build -t jenkins-maven-agent:demo ./agent
```

Start the slave container.

Replace `PASTE_SECRET_HERE` with the secret from Jenkins UI:

```bash
docker run -d \
  --name jenkins-slave-1 \
  --network jenkins-net \
  -v "$DEMO_DIR/java-demo-app:$DEMO_DIR/java-demo-app" \
  jenkins-maven-agent:demo \
  -url http://jenkins-master:8080 \
  -secret PASTE_SECRET_HERE \
  -name docker-slave-1 \
  -webSocket \
  -workDir /home/jenkins/agent
```

Check logs:

```bash
docker logs -f jenkins-slave-1
```

Expected:

```text
Connected
```

Verify from Jenkins UI:

```text
Manage Jenkins → Nodes
```

`docker-slave-1` should be online.

---

# Part 5 — Create Job Type 1: Freestyle

Goal:

Build the Java app using a Freestyle job.

Create job:

```text
Dashboard → New Item
```

Name:

```text
demo-01-freestyle-java
```

Type:

```text
Freestyle project
```

Click `OK`.

Enable:

```text
Restrict where this project can be run
```

Label Expression:

```text
docker && linux && maven
```

Build Steps:

```text
Add build step → Execute shell
```

Script:

```bash
echo "Running Freestyle Java build"
echo "Node: $NODE_NAME"
echo "Workspace: $WORKSPACE"

cd REPLACE_WITH_DEMO_DIR/java-demo-app
mvn -B clean package
java -jar target/jenkins-java-demo-1.0.0.jar

mkdir -p "$WORKSPACE/dist"
cp target/jenkins-java-demo-1.0.0.jar "$WORKSPACE/dist/"
```

Important:

Replace `REPLACE_WITH_DEMO_DIR` with the output of:

```bash
echo $DEMO_DIR
```

Example:

```bash
cd /home/ahmed/jenkins-manual-container-demo/java-demo-app
```

Post-build Actions:

```text
Archive the artifacts
```

Files:

```text
dist/*.jar
```

Save.

Click:

```text
Build Now
```

Explain:

- Freestyle is configured from Jenkins UI.
- It is easy for simple tasks.
- But it is not good for complex CI/CD because pipeline logic is not versioned in Git.

---

# Part 6 — Create Job Type 2: Pipeline

Goal:

Build the Java app using Pipeline syntax.

Create job:

```text
Dashboard → New Item
```

Name:

```text
demo-02-pipeline-java
```

Type:

```text
Pipeline
```

Click `OK`.

Pipeline section:

```text
Definition: Pipeline script
```

Paste this pipeline:

```groovy
pipeline {
    agent { label 'docker && linux && maven' }

    stages {
        stage('Prepare') {
            steps {
                sh '''
                    echo "Node: $NODE_NAME"
                    echo "Workspace: $WORKSPACE"
                    java -version
                    mvn -version
                '''
            }
        }

        stage('Build') {
            steps {
                sh '''
                    cd REPLACE_WITH_DEMO_DIR/java-demo-app
                    mvn -B clean package
                    mkdir -p "$WORKSPACE/dist"
                    cp target/jenkins-java-demo-1.0.0.jar "$WORKSPACE/dist/"
                '''
            }
        }

        stage('Run App') {
            steps {
                sh '''
                    cd REPLACE_WITH_DEMO_DIR/java-demo-app
                    java -jar target/jenkins-java-demo-1.0.0.jar
                '''
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'dist/*.jar', fingerprint: true
            }
        }
    }
}
```

Replace `REPLACE_WITH_DEMO_DIR` with your real demo path.

Save.

Click:

```text
Build Now
```

Explain:

- Pipeline is CI/CD as code.
- Stages are clear: Prepare, Build, Run App, Archive.
- In real projects, this pipeline should be stored in a `Jenkinsfile`.

---

# Part 7 — Create Job Type 3: Multibranch Pipeline

Goal:

Show Jenkins discovering branches automatically from Git.

This demo repository already has:

```text
main
feature/demo-change
```

The repository path is:

```bash
echo "$DEMO_DIR/java-demo-app"
```

Create job:

```text
Dashboard → New Item
```

Name:

```text
demo-03-multibranch-java
```

Type:

```text
Multibranch Pipeline
```

Click `OK`.

Branch Sources:

```text
Add source → Git
```

Project Repository:

```text
REPLACE_WITH_DEMO_DIR/java-demo-app
```

Example:

```text
/home/ahmed/jenkins-manual-container-demo/java-demo-app
```

Credentials:

```text
- none -
```

Build Configuration:

```text
Mode: by Jenkinsfile
Script Path: Jenkinsfile
```

Save.

Click:

```text
Scan Multibranch Pipeline Now
```

Expected:

- Jenkins discovers `main`.
- Jenkins discovers `feature/demo-change`.
- Jenkins creates a child job for each branch.
- Jenkins runs the `Jenkinsfile` from each branch.

Explain:

- Multibranch Pipeline is used when a repository has multiple active branches.
- Jenkins automatically creates branch jobs.
- Each branch can have its own pipeline behavior.
- This is useful for feature branches, release branches, hotfix branches, and merge request validation.

---

# Part 8 — Clean Up

Stop and remove slave:

```bash
docker rm -f jenkins-slave-1
```

Stop and remove Jenkins controller:

```bash
docker rm -f jenkins-master
```

Remove Jenkins data:

```bash
docker volume rm jenkins_home
```

Remove network:

```bash
docker network rm jenkins-net
```

Optional: remove agent image:

```bash
docker rmi jenkins-maven-agent:demo
```

---

# Instructor Summary

Use this comparison while teaching:

| Job type | Where config lives | Best use |
|---|---|---|
| Freestyle | Jenkins UI | Simple jobs and quick demos |
| Pipeline | Jenkins UI or Jenkinsfile | CI/CD as code with stages |
| Multibranch Pipeline | Jenkinsfile per branch | Automatic branch discovery and branch-based CI |
