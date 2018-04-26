package io.openshift.booster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.resolver.ResolverProvider;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.AddressResolver;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.TwitterAuth;
import io.vertx.ext.web.client.*;
import io.vertx.core.buffer.Buffer;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApplication extends AbstractVerticle {

  private static final String template = "Hello, %s!";

  private boolean online = false;

  @Override
  public void start(Future<Void> future) {
      //System.setProperty("vertx.disableDnsResolver", "true");
      
 //     AddressResolver adr = new AddressResolver(vertx, 

    Router router = Router.router(vertx);
    

    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx)
        .register("server-online", fut -> fut.complete(online ? Status.OK() : Status.KO()));

    router.get("/api/greeting").handler(this::greeting);
    router.get("/api/stop").handler(this::stopTheService);
    router.get("/api/health/readiness").handler(rc -> rc.response().end("OK"));
    router.get("/api/health/liveness").handler(healthCheckHandler);
    router.get("/api/foo").handler(this::foo);
    router.get("/").handler(StaticHandler.create());

    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(
        config().getInteger("http.port", 8080), ar -> {
          online = ar.succeeded();
          future.handle(ar.mapEmpty());
        });
  }

  private void stopTheService(RoutingContext rc) {
    rc.response().end("Stopping HTTP server, Bye bye world !");
    online = false;
  }
  
  private void foo(RoutingContext rc) {
System.setProperty("vertx.disableDnsResolver", "true");
      WebClient client = WebClient.create(vertx);
      
      client
        .get(443,"api.github.com", "/users/qodfathr")
        //.postAbs("https://api.github.com/users/qodfathr")
        //.postAbs("https://api.github.com/users/ccamel")
        .ssl(true)
        .send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                rc.response().end(response.bodyAsString());
            } else {
                rc.response().end("FAIL: " + ar.cause().getMessage());
            }
        });
//    final HttpClient httpClient = vertx.createHttpClient();
//    final String url = "https://api.github.com/users/qodfathr";
//    httpClient.get(url, response -> {
//        if (response.statusCode() != 200) {
//            System.err.println("fail");
//        } else {
//            rc.response().end(response.bodyHandler(b -> System.out.println(b.toString())).toString());
//        }
//    }).end();
//    OAuth2Auth oauth2 = TwitterAuth.create(vertx, "clientID", "clientSECRET");
//oauth2.
  }

  private void greeting(RoutingContext rc) {
    if (!online) {
      rc.response().setStatusCode(400).putHeader(CONTENT_TYPE, "text/plain").end("Not online");
      return;
    }

    String name = rc.request().getParam("name");
    if (name == null) {
      name = "World";
    }

    JsonObject response = new JsonObject()
        .put("content", String.format(template, name));

    rc.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(response.encodePrettily());
  }
}
