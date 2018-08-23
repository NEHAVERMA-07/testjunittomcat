/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2018 <mickael.jeanroy@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.mjeanroy.junit.servers.jupiter;

import com.github.mjeanroy.junit.servers.annotations.TestHttpClient;
import com.github.mjeanroy.junit.servers.annotations.TestServer;
import com.github.mjeanroy.junit.servers.annotations.TestServerConfiguration;
import com.github.mjeanroy.junit.servers.client.HttpClient;
import com.github.mjeanroy.junit.servers.engine.AnnotationsHandlerTestLifeCycleAdapter;
import com.github.mjeanroy.junit.servers.engine.EmbeddedServerTestLifeCycleAdapter;
import com.github.mjeanroy.junit.servers.engine.Servers;
import com.github.mjeanroy.junit.servers.servers.AbstractConfiguration;
import com.github.mjeanroy.junit.servers.servers.EmbeddedServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static com.github.mjeanroy.junit.servers.commons.Preconditions.notNull;

/**
 * Extension for Junit Jupiter.
 *
 * This jupiter will:
 *
 * <ul>
 *   <li>Inject an embedded server <strong>before all</strong> tests and stop it <strong>after all</strong> tests.</li>
 *   <li>Read server configuration annotated wiith {@link TestServerConfiguration}</li>
 *   <li>
 *     Resolve parameters of type (or any parameters inheriting from):
 *     <ul>
 *       <li>{@link EmbeddedServer}</li>
 *       <li>{@link AbstractConfiguration}</li>
 *       <li>{@link HttpClient}</li>
 *     </ul>
 *   </li>
 *   <li>
 *     Inject class field annotated with:
 *     <ul>
 *       <li>{@link TestServer}</li>
 *       <li>{@link TestServerConfiguration}</li>
 *       <li>{@link TestHttpClient}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <strong>Note that it is strongly recommended to use parameter instead instead of class injection.</strong>
 *
 * For example:
 *
 * <pre><code>
 *   &#064;ExtendWith(JunitServerExtension.class)
 *  public class MyTest {
 *
 *    &#064;Test
 *    void testGET(HttpClient client) {
 *      HttpResponse rsp = client.prepareGet("/path")
 *        .acceptJson()
 *        .execute();
 *
 *      Assert.assertTrue(rsp.status() == 200);
 *    }
 *  }
 * </code></pre>
 *
 * Note that this jupiter can also be used using {@link RegisterExtension}.
 */
public class JunitServerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
	/**
	 * The namespace where each jupiter variable will be stored.
	 */
	private static final Namespace NAMESPACE = Namespace.create(JunitServerExtension.class.getName());

	/**
	 * The name of the {@link EmbeddedServerTestLifeCycleAdapter} instance in the internal store.
	 */
	private static final String SERVER_ADAPTER_KEY = "serverAdapter";

	/**
	 * The name of the {@link AnnotationsHandlerTestLifeCycleAdapter} instance in the internal store.
	 */
	private static final String ANNOTATIONS_ADAPTER_KEY = "annotationsAdapter";

	/**
	 * The list of parameter resolvers.
	 */
	private static final Map<Class<?>, ParameterResolverFunction> RESOLVERS = new HashMap<Class<?>, ParameterResolverFunction>() {{
		put(EmbeddedServer.class, EmbeddedServerParameterResolverFunction.getInstance());
		put(AbstractConfiguration.class, ConfigurationResolverFunction.getInstance());
		put(HttpClient.class, HttpClientParameterResolverFunction.getInstance());
	}};

	/**
	 * The embedded server to use.
	 */
	private final EmbeddedServer<?> server;

	/**
	 * The embedded server configuration when instantiating it.
	 */
	private final AbstractConfiguration configuration;

	/**
	 * Create the jupiter with default server that will be automatically detected using the Service Provider
	 * API.
	 */
	public JunitServerExtension() {
		this.server = null;
		this.configuration = null;
	}

	/**
	 * Create the jupiter with given server to start/stop before/after tests.
	 *
	 * @param server The embedded server to use.
	 * @throws NullPointerException If {@code server} is {@code null}.
	 */
	public JunitServerExtension(EmbeddedServer<?> server) {
		this.server = notNull(server, "server");
		this.configuration = null;
	}

	/**
	 * Create the jupiter with given server configuration.
	 *
	 * @param configuration The embedded server configuration to use.
	 * @throws NullPointerException If {@code configuration} is {@code null}.
	 */
	public JunitServerExtension(AbstractConfiguration configuration) {
		this.server = null;
		this.configuration = configuration;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		EmbeddedServer<?> server = this.server == null ? instantiateServer(testClass, configuration) : this.server;

		EmbeddedServerTestLifeCycleAdapter serverAdapter = new EmbeddedServerTestLifeCycleAdapter(server);
		serverAdapter.beforeAll();

		putEmbeddedServerAdapterInStore(context, serverAdapter);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		try {
			EmbeddedServerTestLifeCycleAdapter serverAdapter = findEmbeddedServerAdapterInStore(context);
			serverAdapter.afterAll();
		} finally {
			removeEmbeddedServerAdapterFromStore(context);
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		EmbeddedServerTestLifeCycleAdapter serverAdapter = findEmbeddedServerAdapterInStore(context);

		EmbeddedServer<?> server = serverAdapter.getServer();
		AbstractConfiguration configuration = server.getConfiguration();
		AnnotationsHandlerTestLifeCycleAdapter annotationsAdapter = new AnnotationsHandlerTestLifeCycleAdapter(server, configuration);
		annotationsAdapter.beforeEach(context.getRequiredTestInstance());

		putAnnotationsHandlerAdapterInStore(context, annotationsAdapter);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		try {
			Object target = context.getRequiredTestInstance();
			AnnotationsHandlerTestLifeCycleAdapter annotationsAdapter = findAnnotationsHandlerAdapterInStore(context);
			annotationsAdapter.afterEach(target);
		} finally {
			removeAnnotationsHandlerAdapterFromStore(context);
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		final Parameter parameter = parameterContext.getParameter();
		final Class<?> parameterClass = parameter.getType();

		// Fast case: a perfect match.
		if (RESOLVERS.containsKey(parameterClass)) {
			return true;
		}

		for (Class<?> klass : RESOLVERS.keySet()) {
			if (klass.isAssignableFrom(parameterClass)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		final Parameter parameter = parameterContext.getParameter();
		final Class<?> parameterClass = parameter.getType();
		final EmbeddedServerTestLifeCycleAdapter serverAdapter = findEmbeddedServerAdapterInStore(extensionContext);

		// Fast case: a perfect match.
		if (RESOLVERS.containsKey(parameterClass)) {
			return RESOLVERS.get(parameterClass).resolve(parameterContext, serverAdapter);
		}

		for (Class<?> klass : RESOLVERS.keySet()) {
			if (klass.isAssignableFrom(parameterClass)) {
				return RESOLVERS.get(klass).resolve(parameterContext, serverAdapter);
			}
		}

		// Should not happen since Junit framework will call this method if, and only if, the
		// method `supportsParameter` has previously returned `true`.
		return null;
	}

	/**
	 * Instantiate server (implementation to use is automatically detected using the Service Provider
	 * API).
	 *
	 * @param testClass The test class instance.
	 * @param configuration The embedded server configuration to use.
	 * @return The embedded server.
	 */
	protected EmbeddedServer<?> instantiateServer(Class<?> testClass, AbstractConfiguration configuration) {
		return configuration == null ? Servers.instantiate(testClass) : Servers.instantiate(configuration);
	}

	/**
	 * Find {@link EmbeddedServerTestLifeCycleAdapter} instance in the test context store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @return The current stored adapter.
	 */
	private static EmbeddedServerTestLifeCycleAdapter findEmbeddedServerAdapterInStore(ExtensionContext context) {
		return (EmbeddedServerTestLifeCycleAdapter) findInStore(context, SERVER_ADAPTER_KEY);
	}

	/**
	 * Put {@link EmbeddedServerTestLifeCycleAdapter} instance in the test context store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @param serverAdapter The instance to store.
	 */
	private static void putEmbeddedServerAdapterInStore(ExtensionContext context, EmbeddedServerTestLifeCycleAdapter serverAdapter) {
		putInStore(context, SERVER_ADAPTER_KEY, serverAdapter);
	}

	/**
	 * Remove {@link EmbeddedServerTestLifeCycleAdapter} instance from the test context store.
	 *
	 * @param context The Junit-Jupiter test context.
	 */
	private static void removeEmbeddedServerAdapterFromStore(ExtensionContext context) {
		removeFromStore(context, SERVER_ADAPTER_KEY);
	}

	/**
	 * Find {@link AnnotationsHandlerTestLifeCycleAdapter} instance in the test context store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @return The current stored adapter.
	 */
	private static AnnotationsHandlerTestLifeCycleAdapter findAnnotationsHandlerAdapterInStore(ExtensionContext context) {
		return findInStore(context, ANNOTATIONS_ADAPTER_KEY);
	}

	/**
	 * Put {@link AnnotationsHandlerTestLifeCycleAdapter} instance in the test context store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @param annotationsHandlerAdapter The instance to store.
	 */
	private static void putAnnotationsHandlerAdapterInStore(ExtensionContext context, AnnotationsHandlerTestLifeCycleAdapter annotationsHandlerAdapter) {
		putInStore(context, ANNOTATIONS_ADAPTER_KEY, annotationsHandlerAdapter);
	}

	/**
	 * Remove {@link AnnotationsHandlerTestLifeCycleAdapter} instance from the test context store.
	 *
	 * @param context The Junit-Jupiter test context.
	 */
	private static void removeAnnotationsHandlerAdapterFromStore(ExtensionContext context) {
		removeFromStore(context, ANNOTATIONS_ADAPTER_KEY);
	}

	/**
	 * Put value in the internal store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @param name The name of the value to look for.
	 * @param value The value to store.
	 * @param <T> The type of the value to look for.
	 */
	@SuppressWarnings("unchecked")
	private static <T> void putInStore(ExtensionContext context, String name, T value) {
		getStore(context).put(name, value);
	}

	/**
	 * Find value in the internal store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @param name The name of the value to look for.
	 * @param <T> The type of the value to look for.
	 * @return The value currently stored.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T findInStore(ExtensionContext context, String name) {
		return (T) getStore(context).get(name);
	}

	/**
	 * Remove given entry from the internal store.
	 *
	 * @param context The Junit-Jupiter test context.
	 * @param name The entry name to remove.
	 */
	private static void removeFromStore(ExtensionContext context, String name) {
		getStore(context).remove(name);
	}

	/**
	 * Get the internal store from the Junit-Jupiter test context.
	 *
	 * @param extensionContext The test context.
	 * @return The store.
	 */
	private static Store getStore(ExtensionContext extensionContext) {
		return extensionContext.getStore(NAMESPACE);
	}
}
