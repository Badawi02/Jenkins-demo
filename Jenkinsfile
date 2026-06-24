pipeline {
    agent { label 'docker-slave-1' }
    
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

        stage('Clone Repository') {
            steps {
                git branch: "dev",
                    credentialsId: 'Badawi_cred',
                    url: 'https://github.com/Badawi02/Jenkins-demo.git'
            }
        }

        stage('Build') {
            steps {
                sh '''
                    cd java-demo-app
                    mvn -B clean package
                    mkdir -p "$WORKSPACE/dist"
                    cp /home/jenkins/agent/workspace/demo-02-pipeline-java/java-demo-app/target/Jenkins-java-demo-1.0.0.jar "$WORKSPACE/dist/"
                '''
            }
        }

        stage('Run App') {
            steps {
                sh '''
                    cd /home/jenkins/agent/workspace/demo-02-pipeline-java/java-demo-app/target
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
