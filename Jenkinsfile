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

        stage('Build') {
            steps {
                sh '''
                    cd /mnt/sda4/Kimit/Jenkins/java-demo-app
                    mvn -B clean package
                    mkdir -p "$WORKSPACE/dist"
                    cp target//mnt/sda4/Kimit/Jenkins-java-demo-1.0.0.jar "$WORKSPACE/dist/"
                '''
            }
        }

        stage('Run App') {
            steps {
                sh '''
                    cd /mnt/sda4/Kimit/Jenkins/java-demo-app
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