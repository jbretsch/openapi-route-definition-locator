# Sample Apps using the OpenAPI Route Definition Locator

Here you can find an example of running two microservices behind a Spring Cloud Gateway using
the [OpenAPI Route Definition Locator](../README.md) in a Kubernetes cluster. This example
includes Grafana dashboards for monitoring the OpenAPI Route Definition Locator.

## Prerequisites

Install the following software. Make sure their installed binaries are in your `$PATH`.

1. [Kubernetes](https://kubernetes.io) (e.g. via [Docker Desktop](https://www.docker.com/products/docker-desktop/))
2. [Helm](https://helm.sh)
3. [Task](https://taskfile.dev)

For a nice terminal based user interface to manage your Kubernetes cluster, you may want to install
[k9s](https://k9scli.io).

## Build and Deploy

Run in your shell:
```shell
cd sample-apps
task build deploy
```

See the [troubleshooting](#troubleshooting) tips if the Helm deployments fail.

After the deployment succeeded you will see output like this:
```
API Gateway:
  Base URL: http://api.127.0.0.1.nip.io

  Try:
    curl -v http://api.127.0.0.1.nip.io/users | jq .
    curl -v http://api.127.0.0.1.nip.io/users/6ac8d69c-7a8c-4ce3-854a-2a51f8bbd868 | jq .
    curl -v http://api.127.0.0.1.nip.io/users/6ac8d69c-7a8c-4ce3-854a-2a51f8bbd868/orders | jq .
    curl -v http://api.127.0.0.1.nip.io/users/6ac8d69c-7a8c-4ce3-854a-2a51f8bbd868/orders/271acbc1-50b0-45ae-ad04-a231f1057714 | jq .
    curl -v http://api.127.0.0.1.nip.io/actuator/gateway/routes | jq .

Grafana:
  URL              : http://grafana.127.0.0.1.nip.io/
  Login credentials: admin // admin
  Gateway Dashboard: http://grafana.127.0.0.1.nip.io/d/c09a9f35
  Service Dashboard: http://grafana.127.0.0.1.nip.io/d/179dd90b
```

## API Requests via API Gateway

You can send some API requests via the API gateway to the example services:
```shell
curl -v http://api.127.0.0.1.nip.io/users
curl -v http://api.127.0.0.1.nip.io/users/6ac8d69c-7a8c-4ce3-854a-2a51f8bbd868
curl -v http://api.127.0.0.1.nip.io/users/6ac8d69c-7a8c-4ce3-854a-2a51f8bbd868/orders
curl -v http://api.127.0.0.1.nip.io/users/6ac8d69c-7a8c-4ce3-854a-2a51f8bbd868/orders/271acbc1-50b0-45ae-ad04-a231f1057714
```

## Grafana Dashboards

There are Grafana dashboards you can look at. Open <http://grafana.127.0.0.1.nip.io/>. Login with
the credentials `admin` / `admin`.

There is a [Spring Boot Dashboard](http://grafana.127.0.0.1.nip.io/d/179dd90b) and
a [Spring Cloud Gateway Dashboard](http://grafana.127.0.0.1.nip.io/d/c09a9f35).

## Troubleshooting

The Helm deployment of
[kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)
may fail if you have incompatible versions of the CRDs created by this chart installed in your
Kubernetes cluster. Run `task clean` to have them deleted. Then run `task deploy` again.


