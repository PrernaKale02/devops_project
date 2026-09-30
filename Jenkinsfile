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

        stage('Deploy') {
            steps {
                script {
                    if (isUnix()) {
                        error 'The Week 8 local deployment requires a Windows Jenkins agent.'
                    } else {
                        bat 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\deploy.ps1'
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