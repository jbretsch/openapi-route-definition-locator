defaultTasks("clean", "build")

tasks.wrapper {
    gradleVersion = "9.8.1"
    distributionType = Wrapper.DistributionType.ALL
}
