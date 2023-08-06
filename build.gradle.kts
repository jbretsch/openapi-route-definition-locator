defaultTasks("clean", "build")

tasks.wrapper {
    gradleVersion = "8.2.1"
    distributionType = Wrapper.DistributionType.ALL
}
