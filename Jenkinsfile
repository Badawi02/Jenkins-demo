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
                    credentialsId: 'badawi_cred',
                    url: 'https://github.com/Badawi02/Jenkins-demo.git'
            }
        }

        stage('Build') {
            steps {
                sh '''
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