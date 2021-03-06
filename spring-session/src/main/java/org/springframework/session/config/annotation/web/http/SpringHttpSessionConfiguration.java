/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.session.config.annotation.web.http;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.session.web.http.CookieHttpSessionStrategy;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionStrategy;
import org.springframework.session.web.http.MultiHttpSessionStrategy;
import org.springframework.session.web.http.SessionEventHttpSessionListenerAdapter;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Configures the basics for setting up Spring Session in a web environment. In order to
 * use it, you must provide a {@link SessionRepository}. For example:
 *
 * <pre>
 * {@literal @Configuration}
 * {@literal @EnableSpringHttpSession}
 * public class SpringHttpSessionConfig {
 *
 *     {@literal @Bean}
 *     public MapSessionRepository sessionRepository() {
 *         return new MapSessionRepository();
 *     }
 *
 * }
 * </pre>
 *
 * <p>
 * It is important to note that no infrastructure for session expirations is configured
 * for you out of the box. This is because things like session expiration are highly
 * implementation dependent. This means if you require cleaning up expired sessions, you
 * are responsible for cleaning up the expired sessions.
 * </p>
 *
 * <p>
 * The following is provided for you with the base configuration:
 * </p>
 *
 * <ul>
 * <li>SessionRepositoryFilter - is responsible for wrapping the HttpServletRequest with
 * an implementation of HttpSession that is backed by a SessionRepository</li>
 * <li>SessionEventHttpSessionListenerAdapter - is responsible for translating Spring
 * Session events into HttpSessionEvent. In order for it to work, the implementation of
 * SessionRepository you provide must support {@link SessionCreatedEvent} and
 * {@link SessionDestroyedEvent}.</li>
 * <li>
 * </ul>
 *
 * @author Rob Winch
 * @author Vedran Pavic
 * @since 1.1
 *
 * @see EnableSpringHttpSession
 */
@Configuration
public class SpringHttpSessionConfiguration implements ApplicationContextAware {

	private CookieHttpSessionStrategy defaultHttpSessionStrategy = new CookieHttpSessionStrategy();

	private boolean usesSpringSessionRememberMeServices;

	private ServletContext servletContext;

	private CookieSerializer cookieSerializer;

	private HttpSessionStrategy httpSessionStrategy = this.defaultHttpSessionStrategy;

	private List<HttpSessionListener> httpSessionListeners = new ArrayList<>();

	@PostConstruct
	public void init() {
		if (this.cookieSerializer != null) {
			this.defaultHttpSessionStrategy.setCookieSerializer(this.cookieSerializer);
		}
		else if (this.usesSpringSessionRememberMeServices) {
			DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
			cookieSerializer.setRememberMeRequestAttribute(
					SpringSessionRememberMeServices.REMEMBER_ME_LOGIN_ATTR);
			this.defaultHttpSessionStrategy.setCookieSerializer(cookieSerializer);
		}
	}

	@Bean
	public SessionEventHttpSessionListenerAdapter sessionEventHttpSessionListenerAdapter() {
		return new SessionEventHttpSessionListenerAdapter(this.httpSessionListeners);
	}

	@Bean
	public <S extends ExpiringSession> SessionRepositoryFilter<? extends ExpiringSession> springSessionRepositoryFilter(
			SessionRepository<S> sessionRepository) {
		SessionRepositoryFilter<S> sessionRepositoryFilter = new SessionRepositoryFilter<>(
				sessionRepository);
		sessionRepositoryFilter.setServletContext(this.servletContext);
		if (this.httpSessionStrategy instanceof MultiHttpSessionStrategy) {
			sessionRepositoryFilter.setHttpSessionStrategy(
					(MultiHttpSessionStrategy) this.httpSessionStrategy);
		}
		else {
			sessionRepositoryFilter.setHttpSessionStrategy(this.httpSessionStrategy);
		}
		return sessionRepositoryFilter;
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (ClassUtils.isPresent(
				"org.springframework.security.web.authentication.RememberMeServices",
				null)) {
			this.usesSpringSessionRememberMeServices = !ObjectUtils
					.isEmpty(applicationContext
							.getBeanNamesForType(SpringSessionRememberMeServices.class));
		}
	}

	@Autowired(required = false)
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Autowired(required = false)
	public void setCookieSerializer(CookieSerializer cookieSerializer) {
		this.cookieSerializer = cookieSerializer;
	}

	@Autowired(required = false)
	public void setHttpSessionStrategy(HttpSessionStrategy httpSessionStrategy) {
		this.httpSessionStrategy = httpSessionStrategy;
	}

	@Autowired(required = false)
	public void setHttpSessionListeners(List<HttpSessionListener> listeners) {
		this.httpSessionListeners = listeners;
	}

}
