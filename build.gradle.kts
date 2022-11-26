defaultTasks("clean", "build")

tasks.wrapper {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.ALL
}
