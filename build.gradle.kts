defaultTasks("clean", "build")

tasks.wrapper {
    gradleVersion = "8.14.3"
    distributionType = Wrapper.DistributionType.ALL
}
