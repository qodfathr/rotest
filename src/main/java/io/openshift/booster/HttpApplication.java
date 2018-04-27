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
import io.vertx.core.json.JsonArray;

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
    router.get("/api/bar").handler(this::bar);
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
//System.setProperty("vertx.disableDnsResolver", "true");
      WebClient client = WebClient.create(vertx);
      
      client
        .get(443,"api.github.com", "/users/qodfathr")
        .ssl(true)
        .send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                JsonObject gitHubUser = new JsonObject(response.bodyAsString());
                rc.response().end(gitHubUser.getString("avatar_url"));
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

  private void bar(RoutingContext rc) {
//      WebClientOptions wco = new WebClientOptions()
  //      .setConnectTimeout(0)
      WebClient client = WebClient.create(vertx);
      
      client
        .post(443,"ussouthcentral.services.azureml.net", "/workspaces/9dbf016f411f4388b7a574524b137656/services/954b60a6ae1c4903a9751a2a17ff988f/execute")
        .putHeader("Content-Type", "application/json")
        .addQueryParam("api-version", "2.0")
        .addQueryParam("format", "swagger")
        .ssl(true)
        .sendJsonObject(new JsonObject("{\"Inputs\": {\"input1\": [{\"sentiment_label\":\"2\",\"tweet_text\":\"have a nice day\"}]},\"GlobalParameters\": {}}")
            , ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    //rc.response().end(response.bodyAsString());
                    JsonObject sentimentResult = new JsonObject(response.bodyAsString());
                    JsonObject xyz = sentimentResult.getJsonObject("Results");
                    JsonArray xyz2 = xyz.getJsonArray("output1");// .getJsonObject("output1");
                    JsonObject xyz3 = xyz2.getJsonObject(0);
                    rc.response().end(xyz3.getString("Sentiment") + " : " + xyz3.getString("Score"));
                    //rc.response().end(gitHubUser.getString("avatar_url"));
                } else {
                    rc.response().end("FAIL: " + ar.cause().getMessage());
            }
        });
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
