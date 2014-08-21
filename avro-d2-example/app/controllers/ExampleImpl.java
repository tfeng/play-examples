package controllers;

import org.springframework.stereotype.Component;

import controllers.protocols.Example;

@Component("example")
public class ExampleImpl implements Example {

  @Override
  public String echo(String message) {
    return message;
  }
}
