import me.tfeng.play.plugins.SpringGlobalSettings;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.ClientAuthenticationException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

import play.libs.F.Promise;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

public class Global extends SpringGlobalSettings {

  @Override
  public Promise<Result> onError(RequestHeader request, Throwable t) {
    Throwable cause = t.getCause();
    if (cause instanceof AccessDeniedException
        || cause instanceof AuthenticationException
        || cause instanceof ClientAuthenticationException
        || cause instanceof ClientRegistrationException) {
      return Promise.pure(Results.unauthorized());
    } else if (cause instanceof OAuth2Exception) {
      return Promise.pure(Results.status(((OAuth2Exception) cause).getHttpErrorCode()));
    } else {
      return Promise.pure(Results.internalServerError());
    }
  }
}
