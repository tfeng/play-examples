package beans;

import org.springframework.stereotype.Component;

import controllers.protocols.Example;

@Component("example")
public class ExampleImpl implements Example {

  @Override
  public CharSequence echo(CharSequence message) {
    return message;
  }
}
