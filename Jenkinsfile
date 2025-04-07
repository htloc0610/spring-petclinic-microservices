pipeline {
    agent any

    environment {
        GITHUB_TOKEN = credentials('github-token') // Đảm bảo bạn có token GitHub trong Jenkins Credentials
    }

    stages {
        stage('Build') {
            steps {
                script {
                    echo "Building project..."
                    // Các bước build ở đây (ví dụ: sh 'mvn clean package')
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    echo "Running tests..."
                    // Các bước test ở đây (ví dụ: sh 'mvn test')
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully!"
            githubChecks(
                name: 'Jenkins CI',
                status: 'COMPLETED',
                conclusion: 'SUCCESS',
                detailsURL: env.BUILD_URL,
                output: [
                    title: 'Build and Tests Passed',
                    summary: 'The build and tests completed successfully.',
                    text: 'Everything is green!'
                ]
            )
        }
        failure {
            echo "Pipeline failed!"
            githubChecks(
                name: 'Jenkins CI',
                status: 'COMPLETED',
                conclusion: 'FAILURE',
                detailsURL: env.BUILD_URL,
                output: [
                    title: 'Build or Tests Failed',
                    summary: 'The build or tests failed.',
                    text: 'Please check the details.'
                ]
            )
        }
        always {
            script {
                echo "Pipeline execution ended with status: ${currentBuild.result}"
            }
        }
    }
}
