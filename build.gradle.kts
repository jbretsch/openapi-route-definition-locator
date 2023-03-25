defaultTasks("clean", "build")

tasks.wrapper {
    gradleVersion = "8.0.2"
    distributionType = Wrapper.DistributionType.ALL
}
