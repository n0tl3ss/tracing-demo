package com.example

import io.jaegertracing.internal.JaegerSpan
import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.metrics.InMemoryMetricsFactory
import io.jaegertracing.internal.reporters.InMemoryReporter
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@MicronautTest
class TracingDemoSpec extends Specification {


    @AutoCleanup
    private ApplicationContext context

    private PollingConditions conditions = new PollingConditions()
    private EmbeddedServer embeddedServer
    private HttpClient client
    private InMemoryReporter reporter


    void setup() {
        context = ApplicationContext
                .builder('tracing.jaeger.enabled': true,
                        'tracing.jaeger.sampler.probability': 1,
                        'tracing.exclusions[0]': '.*hello.*')
                .singletons(
                        new InMemoryReporter(),
                        new InMemoryMetricsFactory())
                .start()

        embeddedServer = context.getBean(EmbeddedServer).start()
        client = context.createBean(HttpClient, embeddedServer.URL)
        reporter = context.getBean(InMemoryReporter)
    }


    void 'test basic response reactive HTTP tracing'() {

        when:
        HttpResponse<String> response = client.toBlocking().exchange('/test', String)

        then:
        response
        conditions.eventually {
            reporter.spans.size() == 2

            JaegerSpan span = reporter.spans.find { it.operationName == 'GET /{name}' }
            span != null
            span.tags['foo'] == 'bar'
            span.tags['http.path'] == '/test'
        }
    }

}
