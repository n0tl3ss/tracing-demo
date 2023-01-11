package com.example;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.opentracing.Tracer;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@Controller
public class TestController {

    @Inject
    Tracer tracer;

    @Get("/{name}")
    HttpResponse<Publisher<String>> responseRx(String name) {
        return HttpResponse.ok(Publishers.map(
                Mono.fromCallable(() -> {
                    if (tracer.activeSpan() != null) {
                        tracer.activeSpan().setTag("foo", "bar");
                    }
                    return name;
                }
            ), (n->n)));
    }
}
