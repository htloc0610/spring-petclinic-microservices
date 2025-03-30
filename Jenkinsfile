pipeline {
    agent any

    environment {
        REPO_URL = 'https://github.com/htloc0610/spring-petclinic-microservices'
        BRANCH = "main"
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Cloning repository ${REPO_URL} - Branch: ${BRANCH}"
                    git branch: "${BRANCH}", url: "${REPO_URL}"
                }
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def changes = sh(script: 'git diff --name-only HEAD~1', returnStdout: true).trim()
                    echo "Files changed:\n${changes}"

                    def services = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-config-server',
                        'spring-petclinic-customers-service',
                        'spring-petclinic-discovery-server',
                        'spring-petclinic-genai-service'
                    ]

                    def affectedServices = changes.tokenize("\n").collect { it.split("/")[0] }.unique().findAll { it in services }

                    if (affectedServices.isEmpty()) {
                        echo "No relevant changes, skipping pipeline"
                        currentBuild.result = 'ABORTED'
                        return
                    }

                    env.AFFECTED_SERVICES = affectedServices.join(",")
                    echo "Services to build: ${env.AFFECTED_SERVICES}"
                }
            }
        }

        stage('Test') {
            when {
                expression { env.AFFECTED_SERVICES != null && env.AFFECTED_SERVICES != "" }
            }
            steps {
                script {
                    env.AFFECTED_SERVICES.split(",").each { service ->
                        echo "Running tests for ${service}..."
                        dir("${service}") {
                            sh './mvnw test'
                        }
                    }
                }
            }
            post {
                always {
                    junit "**/target/surefire-reports/*.xml"
                }
            }
        }

        stage('Code Coverage') {
            when {
                expression { env.AFFECTED_SERVICES != null && env.AFFECTED_SERVICES != "" }
            }
            steps {
                script {
                    env.AFFECTED_SERVICES.split(",").each { service ->
                        echo "Checking test coverage for ${service}..."
                        dir("${service}") {
                            sh './mvnw jacoco:report'
                        }
                    }
                }
            }
            post {
                always {
                    publishHTML([target: [reportDir: "target/site/jacoco", reportFiles: 'index.html', reportName: "Code Coverage"]])
                }
            }
        }

        stage('Build') {
            when {
                expression { env.AFFECTED_SERVICES != null && env.AFFECTED_SERVICES != "" }
            }
            steps {
                script {
                    env.AFFECTED_SERVICES.split(",").each { service ->
                        echo "Building ${service}..."
                        dir("${service}") {
                            sh './mvnw clean package'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully for ${env.AFFECTED_SERVICES}!"
        }
        failure {
            echo "Pipeline failed!"
        }
    }
}
