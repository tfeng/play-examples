package controllers;

import me.tfeng.play.plugins.DustPlugin;

import org.springframework.stereotype.Service;

import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class Application extends Controller {

  public Promise<Result> index(String name) {
    ObjectNode data = Json.newObject();
    data.put("name", name);
    return DustPlugin.getInstance().render("home/index", data).map(content -> Results.ok(content));
  }
}
