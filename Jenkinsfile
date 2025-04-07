stage('Notify GitHub') {
    steps {
        script {
            if (env.CHANGE_ID && env.CHANGE_TARGET == 'main') {
                env.AFFECTED_SERVICES.split(",").each { service ->
                    def coverageFile = "coverage_${service}.txt"
                    if (fileExists(coverageFile)) {
                        def coverageStr = readFile(coverageFile).trim()
                        def coverageVal = coverageStr.toFloat()

                        echo "Notify GitHub: ${service} coverage = ${coverageVal}%"

                        if (coverageVal < 70) {
                            githubChecks(
                                name: "Test Code Coverage - ${service}",
                                status: 'completed',
                                conclusion: 'failure',
                                detailsURL: env.BUILD_URL,
                                output: [
                                    title: 'Code Coverage Check Failed',
                                    summary: "Coverage for ${service} is ${coverageVal}%, which is below 70%."
                                ]
                            )
                            error "Code coverage for ${service} is below threshold"
                        } else {
                            githubChecks(
                                name: "Test Code Coverage - ${service}",
                                status: 'completed',
                                conclusion: 'success',
                                detailsURL: env.BUILD_URL,
                                output: [
                                    title: 'Code Coverage Check Success',
                                    summary: "Coverage for ${service} is ${coverageVal}%"
                                ]
                            )
                        }
                    } else {
                        echo "No coverage file found for ${service}, skipping GitHub notification."
                    }
                }
            } else {
                echo "Skip GitHub notify: Not a PR to main"
            }
        }
    }
}
