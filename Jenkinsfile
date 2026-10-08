pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B clean compile -DskipTests'
                    } else {
                        bat 'mvn -B clean compile -DskipTests'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B test'
                    } else {
                        bat 'mvn -B test'
                    }
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B package -DskipTests'
                    } else {
                        bat 'mvn -B package -DskipTests'
                    }
                }
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    if (isUnix()) {
                        error 'Docker deployment requires a Windows Jenkins agent with Docker Desktop access.'
                    } else {
                        bat 'docker build -t child-education-sponsorship:%BUILD_NUMBER% -t child-education-sponsorship:latest .'
                    }
                }
            }
        }

        stage('Docker Registry Push') {
            steps {
                script {
                    def registryHost = env.DOCKER_REGISTRY_HOST?.trim()
                    def repository = env.DOCKER_REGISTRY_REPOSITORY?.trim()
                    def registryNamespace = env.DOCKER_REGISTRY_NAMESPACE?.trim()
                    def credentialsId = env.DOCKER_REGISTRY_CREDENTIALS?.trim()

                    if (!credentialsId) {
                        echo 'Registry push skipped: configure a Jenkins username/password credential ID in DOCKER_REGISTRY_CREDENTIALS. Docker Hub is the default; DOCKER_REGISTRY_HOST, DOCKER_REGISTRY_NAMESPACE, and DOCKER_REGISTRY_REPOSITORY can override the destination.'
                    } else if (isUnix()) {
                        error 'Docker registry publishing requires a Windows Jenkins agent.'
                    } else {
                        withCredentials([usernamePassword(
                            credentialsId: credentialsId,
                            usernameVariable: 'DOCKER_REGISTRY_USERNAME',
                            passwordVariable: 'DOCKER_REGISTRY_PASSWORD'
                        )]) {
                            withEnv([
                                "DOCKER_REGISTRY_HOST=${registryHost ?: 'docker.io'}",
                                "DOCKER_REGISTRY_NAMESPACE=${registryNamespace ?: ''}",
                                "DOCKER_REGISTRY_REPOSITORY=${repository ?: ''}"
                            ]) {
                                bat 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\push-docker-image.ps1'
                            }
                        }
                    }
                }
            }
        }

        stage('Docker Deployment') {
            steps {
                script {
                    if (isUnix()) {
                        error 'Docker deployment requires a Windows Jenkins agent with Docker Desktop access.'
                    } else {
                        bat 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\deploy-docker.ps1'
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    if (isUnix()) {
                        error 'The application health check requires the Windows Jenkins agent hosting Docker.'
                    } else {
                        bat 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\health-check-docker.ps1'
                    }
                }
            }
        }

        stage('Selenium Tests') {
            steps {
                script {
                    if (isUnix()) {
                        error 'The Selenium tests require the Windows Jenkins agent and Chrome.'
                    } else {
                        bat 'mvn failsafe:integration-test failsafe:verify'
                    }
                }
            }
            post {
                always {
                    junit testResults: 'target/*-reports/TEST-*.xml'
                }
            }
        }
    }
}
