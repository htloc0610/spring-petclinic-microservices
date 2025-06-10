pipeline {
    agent { label 'built-in' }

    environment {
        REPO_URL = 'https://github.com/htloc0610/spring-petclinic-microservices'
        WORKSPACE_DIR = "repo"
        DOCKER_IMAGE_PREFIX = 'anwirisme'

        DEPLOY_REPO = 'https://github.com/htloc0610/petclinic-deploy'
        DEPLOY_DIR = 'petclinic-deploy'
        VALUE_FILE = 'petclinic-deploy/values-dev.yaml'

        GIT_TAG_NAME = sh(script: "git describe --tags --exact-match || true", returnStdout: true).trim()
        STAGING_FILE = 'values-staging.yaml'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "Cloning repository ${REPO_URL}"
                    sh "rm -rf ${WORKSPACE_DIR}"
                    sh "mkdir -p ${WORKSPACE_DIR}"

                    dir(WORKSPACE_DIR) {
                        if (env.CHANGE_ID) {
                            // Pull Request
                            echo "Checking out PR #${env.CHANGE_ID} (target: ${env.CHANGE_TARGET})"
                            sh "git init"
                            sh "git remote add origin ${REPO_URL}"
                            sh "git fetch origin refs/pull/${env.CHANGE_ID}/merge:pr-${env.CHANGE_ID}"
                            sh "git fetch origin ${env.CHANGE_TARGET}:refs/remotes/origin/${env.CHANGE_TARGET}"
                            sh "git checkout pr-${env.CHANGE_ID}"
                        } else {
                            // Branch
                            echo "Checking out branch ${env.BRANCH_NAME}"
                            sh "git clone -b ${env.BRANCH_NAME} ${REPO_URL} ."
                        }
                    }
                }
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    dir(WORKSPACE_DIR) {
                        def isPR = env.CHANGE_ID != null
                        def changes = ''

                        if (isPR) {
                            changes = sh(script: "git diff --name-only origin/${env.CHANGE_TARGET}", returnStdout: true).trim()
                        } else {
                            changes = sh(script: "git diff --name-only HEAD~1", returnStdout: true).trim()
                        }

                        echo "Files changed:\n${changes}"

                        def services = [
                            'spring-petclinic-admin-server',
                            'spring-petclinic-api-gateway',
                            'spring-petclinic-config-server',
                            'spring-petclinic-customers-service',
                            'spring-petclinic-discovery-server',
                            'spring-petclinic-genai-service',
                            'spring-petclinic-vets-service',
                            'spring-petclinic-visits-service'
                        ]

                        def affectedServices = changes.tokenize("\n")
                            .collect { it =~ /^([^\/]+)\// ? (it =~ /^([^\/]+)\//)[0][1] : null }
                            .unique()
                            .findAll { it in services }

                        if (affectedServices.isEmpty()) {
                            echo "No relevant changes, skipping tests and build"
                            env.SKIP_PIPELINE = "true"
                        } else {
                            env.AFFECTED_SERVICES = affectedServices.join(",")
                            echo "Services to build: ${env.AFFECTED_SERVICES}"
                        }
                    }
                }
            }
        }

        stage('Build') {
            when {
                allOf {
                    expression { env.AFFECTED_SERVICES }
                    expression { env.SKIP_PIPELINE != "true" }
                }
            }
            steps {
                script {
                    env.AFFECTED_SERVICES.split(",").each { service ->
                        echo "Building ${service}..."
                        dir("${WORKSPACE_DIR}/${service}") {
                            try {
                                sh 'mvn clean package'
                            } catch (Exception e) {
                                error "Build failed for ${service}"
                            }
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            when {
                allOf {
                    expression { env.AFFECTED_SERVICES }
                    expression { env.SKIP_PIPELINE != "true" }
                    expression {
                        def tagName = sh(script: "git describe --tags --exact-match || true", returnStdout: true).trim()
                        return tagName == ""
                    }
                }
            }
            steps {
                script {
                    dir(WORKSPACE_DIR) {
                        env.DOCKER_COMMIT_ID = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        echo "Commit ID for tagging Docker images: ${env.DOCKER_COMMIT_ID}"

                        env.AFFECTED_SERVICES.split(",").each { service ->
                            echo "Building Docker image for ${service} with tag ${env.DOCKER_COMMIT_ID}..."

                            sh """
                                DOCKER_BUILDKIT=1 ./mvnw clean install -pl ${service} -PbuildDocker \\
                                -Ddocker.image.prefix=${DOCKER_IMAGE_PREFIX} \\
                                -Ddocker.image.tag=${env.DOCKER_COMMIT_ID}
                            """

                            // Add tag after build
                            sh """
                                docker tag ${DOCKER_IMAGE_PREFIX}/${service}:latest ${DOCKER_IMAGE_PREFIX}/${service}:${env.DOCKER_COMMIT_ID}
                            """
                        }
                    }
                }
            }
        }

        stage('Push Docker Images') {
            when {
                allOf {
                    expression { env.AFFECTED_SERVICES }
                    expression { env.SKIP_PIPELINE != "true" }
                    expression {
                        def tagName = sh(script: "git describe --tags --exact-match || true", returnStdout: true).trim()
                        return tagName == ""
                    }
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials-id', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                        """

                        echo "Listing all Docker images:"
                        sh "docker images"

                        dir(WORKSPACE_DIR) {
                            env.AFFECTED_SERVICES.split(",").each { service ->
                                echo "Pushing Docker image for ${service}:${env.DOCKER_COMMIT_ID}..."

                                sh """
                                    docker push ${DOCKER_IMAGE_PREFIX}/${service}:${env.DOCKER_COMMIT_ID}
                                """
                            }
                        }
                        sh "docker logout"
                    }
                }
            }
        }

        stage('Update Helm Image Tags in Deploy Repo') {
            when {
                allOf {
                    expression { env.BRANCH_NAME == 'main' }
                    expression { env.AFFECTED_SERVICES }
                    expression { env.SKIP_PIPELINE != "true" }
                    expression {
                        def tagName = sh(script: "git describe --tags --exact-match || true", returnStdout: true).trim()
                        return tagName == ""
                    }
                }
            }
            steps {
                script {
                    def DEPLOY_REPO = "https://github.com/htloc0610/petclinic-deploy"
                    def DEPLOY_DIR = "petclinic-deploy"
                    def VALUE_FILE = "${DEPLOY_DIR}/values-dev.yaml"

                    echo "Cloning deployment repo..."
                    sh "rm -rf ${DEPLOY_DIR}"
                    sh "git clone ${DEPLOY_REPO} ${DEPLOY_DIR}"

                    echo "===== BEFORE UPDATE ====="
                    sh "cat ${VALUE_FILE}"

                    echo "Updating image tags in ${VALUE_FILE}..."
                    env.AFFECTED_SERVICES.split(",").each { service ->
                        def shortName = service
                            .replace("spring-petclinic-", "")
                            .replace("-service", "Service")
                            .replace("-server", "Server")
                            .replace("-gateway", "Gateway")

                        echo "Updating ${shortName} to tag ${env.DOCKER_COMMIT_ID}..."

                        sh """
                            yq e '.image.${shortName}.tag = "${env.DOCKER_COMMIT_ID}"' -i ${VALUE_FILE}
                        """
                    }

                    echo "===== AFTER UPDATE ====="
                    sh "cat ${VALUE_FILE}"

                    echo "Committing and pushing updated Helm values..."
                    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        dir("${DEPLOY_DIR}") {
                            sh """
                                git config user.email "\${GIT_USER}@users.noreply.github.com"
                                git config user.name "\${GIT_USER}"
                                git remote set-url origin https://\${GIT_USER}:\${GIT_PASS}@github.com/htloc0610/petclinic-deploy.git

                                git add values-dev.yaml
                                git commit -m "chore: update dev image tags to ${env.DOCKER_COMMIT_ID}" || true
                                git push origin main || true
                            """
                        }
                    }
                }
            }
        }

        stage('Build & Push All Services on Git Tag') {
            when {
                expression {
                    env.GIT_TAG_NAME = sh(
                        script: "git describe --tags --exact-match || true",
                        returnStdout: true
                    ).trim()
                    return env.GIT_TAG_NAME?.trim()
                }
            }
            steps {
                script {
                    def allServices = [
                        'spring-petclinic-admin-server',
                        'spring-petclinic-api-gateway',
                        'spring-petclinic-config-server',
                        'spring-petclinic-customers-service',
                        'spring-petclinic-discovery-server',
                        'spring-petclinic-genai-service',
                        'spring-petclinic-vets-service',
                        'spring-petclinic-visits-service'
                    ]

                    dir(WORKSPACE_DIR) {
                        echo "Building and pushing all services for Git tag: ${env.GIT_TAG_NAME}"

                        allServices.each { service ->
                            echo "Building Docker image for ${service} with tag ${env.GIT_TAG_NAME}..."
                            sh """
                                DOCKER_BUILDKIT=1 ./mvnw clean install -pl ${service} -PbuildDocker \\
                                -Ddocker.image.prefix=${DOCKER_IMAGE_PREFIX} \\
                                -Ddocker.image.tag=${env.GIT_TAG_NAME}
                            """

                            sh """
                                docker tag ${DOCKER_IMAGE_PREFIX}/${service}:latest ${DOCKER_IMAGE_PREFIX}/${service}:${env.GIT_TAG_NAME}
                            """
                        }

                        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials-id', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"

                            allServices.each { service ->
                                echo "Pushing Docker image for ${service}:${env.GIT_TAG_NAME}..."
                                sh "docker push ${DOCKER_IMAGE_PREFIX}/${service}:${env.GIT_TAG_NAME}"
                            }

                            sh "docker logout"
                        }
                    }

                    // Cập nhật values-staging.yaml
                    echo "Cloning deployment repo to update staging values..."
                    sh "rm -rf ${DEPLOY_DIR}"
                    sh "git clone ${DEPLOY_REPO} ${DEPLOY_DIR}"

                    def stagingFile = "${DEPLOY_DIR}/${env.STAGING_FILE}"

                    echo "===== BEFORE UPDATE (staging) ====="
                    sh "cat ${stagingFile}"

                    allServices.each { service ->
                        def shortName = service
                            .replace("spring-petclinic-", "")
                            .replace("-service", "Service")
                            .replace("-server", "Server")
                            .replace("-gateway", "Gateway")

                        echo "Updating ${shortName} to tag ${env.GIT_TAG_NAME} in staging..."
                        sh """
                            yq e '.image.${shortName}.tag = "${env.GIT_TAG_NAME}"' -i ${stagingFile}
                        """
                    }

                    echo "===== AFTER UPDATE (staging) ====="
                    sh "cat ${stagingFile}"

                    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        dir("${DEPLOY_DIR}") {
                            sh """
                                git config user.email "\${GIT_USER}@users.noreply.github.com"
                                git config user.name "\${GIT_USER}"
                                git remote set-url origin https://\${GIT_USER}:\${GIT_PASS}@github.com/htloc0610/petclinic-deploy.git

                                git add values-staging.yaml
                                git commit -m "chore: update staging image tags to ${env.GIT_TAG_NAME}" || true
                                git push origin main || true
                            """
                        }
                    }
                }
            }
        }

        stage('Clean Docker Images') {
            when {
                anyOf {
                    expression { env.GIT_TAG_NAME?.trim() }
                    allOf {
                        expression { env.AFFECTED_SERVICES }
                        expression { env.SKIP_PIPELINE != "true" }
                    }
                }
            }
            steps {
                script {
                    echo "Pruning all unused Docker images and containers..."
                    sh "docker system prune -af"
                }
            }
        }


    }

    post {
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed!"
        }
        always {
            script {
                echo "Pipeline execution ended with status: ${currentBuild.result}"
            }
        }
    }
}
