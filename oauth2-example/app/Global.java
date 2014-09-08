/**
 * Copyright 2014 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import me.tfeng.play.spring.SpringGlobalSettings;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.ClientAuthenticationException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

import play.libs.F.Promise;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
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
      return super.onError(request, t);
    }
  }
}
