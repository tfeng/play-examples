package controllers;

import me.tfeng.play.plugins.AvroD2Plugin;
import play.mvc.Result;
import play.mvc.Results;
import controllers.protocols.Example;

public class ClientController {

  public static Result invoke(String message) throws Exception {
    Example proxy = AvroD2Plugin.getInstance().getClient(Example.class);
    return Results.ok(proxy.echo(message));
  }
}
