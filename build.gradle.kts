defaultTasks("clean", "build")

tasks.wrapper {
    gradleVersion = "8.4"
    distributionType = Wrapper.DistributionType.ALL
}
