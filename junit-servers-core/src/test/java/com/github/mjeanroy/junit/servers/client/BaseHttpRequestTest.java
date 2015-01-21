/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 <mickael.jeanroy@gmail.com>
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

package com.github.mjeanroy.junit.servers.client;

import static com.github.mjeanroy.junit.servers.client.BaseHttpRequestTest.HeaderEntry.header;
import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests skeleton for http request implementation.
 */
public abstract class BaseHttpRequestTest {

	@Before
	public void setUp() throws Exception {
		onSetUp();
	}

	@Test
	public void it_should_create_http_request() throws Exception {
		String url = "http://localhost:8080/foo";
		HttpMethod httpMethod = HttpMethod.GET;
		HttpRequest request = createHttpRequest(httpMethod, url);
		checkInternals(request, httpMethod, url);
	}

	@Test
	public void it_should_add_header() throws Exception {
		HttpRequest request = createDefaultRequest();

		String headerName = "foo";
		String headerValue = "bar";
		request.addHeader(headerName, headerValue);

		checkHeader(request, headerName, headerValue);
	}

	@Test
	public void it_should_add_header_requested_with() throws Exception {
		HttpRequest request = createDefaultRequest();
		request.asXmlHttpRequest();
		checkHeader(request, "X-Requested-With", "XMLHttpRequest");
	}

	@Test
	public void it_should_add_header_content_type_json() throws Exception {
		HttpRequest request = createDefaultRequest();
		request.asJson();
		checkHeader(request, "Content-Type", "application/json");
	}

	@Test
	public void it_should_add_header_content_type_xml() throws Exception {
		HttpRequest request = createDefaultRequest();
		request.asXml();
		checkHeader(request, "Content-Type", "application/xml");
	}

	@Test
	public void it_should_add_header_content_type_form_url_encoded() throws Exception {
		HttpRequest request = createDefaultRequest();
		request.asFormUrlEncoded();
		checkHeader(request, "Content-Type", "application/x-www-form-urlencoded");
	}

	@Test
	public void it_should_add_header_accept_json() throws Exception {
		HttpRequest request = createDefaultRequest();
		request.acceptJson();
		checkHeader(request, "Accept", "application/json");
	}

	@Test
	public void it_should_add_header_accept_xml() throws Exception {
		HttpRequest request = createDefaultRequest();
		request.acceptXml();
		checkHeader(request, "Accept", "application/xml");
	}

	@Test
	public void it_should_add_query_params() throws Exception {
		HttpRequest request = createDefaultRequest();

		String paramName = "foo";
		String paramValue = "bar";
		request.addQueryParam(paramName, paramValue);

		checkQueryParam(request, paramName, paramValue);
	}

	@Test
	public void it_should_execute_request() throws Exception {
		HttpRequest request = createDefaultRequest();

		HttpResponse httpResponse = fakeExecution(request, new ExecutionStrategy() {
			@Override
			public HttpResponse execute(HttpRequest request) {
				return request.execute();
			}
		});

		assertThat(httpResponse).isNotNull();
		checkExecution(httpResponse);
	}

	@Test
	public void it_should_execute_request_as_json() throws Exception {
		HttpRequest request = createDefaultRequest();

		HttpResponse httpResponse = fakeExecution(request, new ExecutionStrategy() {
			@Override
			public HttpResponse execute(HttpRequest request) {
				return request.executeJson();
			}
		});

		assertThat(httpResponse).isNotNull();
		checkExecution(httpResponse,
				header("Content-Type", "application/json"),
				header("Accept", "application/json")
		);
	}

	@Test
	public void it_should_execute_request_as_xml() throws Exception {
		HttpRequest request = createDefaultRequest();

		HttpResponse httpResponse = fakeExecution(request, new ExecutionStrategy() {
			@Override
			public HttpResponse execute(HttpRequest request) {
				return request.executeXml();
			}
		});

		assertThat(httpResponse).isNotNull();
		checkExecution(httpResponse,
				header("Content-Type", "application/xml"),
				header("Accept", "application/xml")
		);
	}

	/**
	 * Should create mock data during test setup.
	 *
	 * @throws Exception
	 */
	protected abstract void onSetUp() throws Exception;

	/**
	 * Should create http request implementation.
	 * This http request will be used to check http request
	 * constructor.
	 *
	 * @param httpMethod Http method.
	 * @param url Http request url.
	 * @return Created http request.
	 * @throws Exception
	 */
	protected abstract HttpRequest createHttpRequest(HttpMethod httpMethod, String url) throws Exception;

	/**
	 * Should check that created http request is valid according
	 * to given http method and url.
	 * It should also check that internal data are valid (probably empty
	 * query params and empty headers).
	 *
	 * @param request Created http request.
	 * @param httpMethod Http method used to create http request.
	 * @param url Url used to create http request.
	 * @throws Exception
	 */
	protected abstract void checkInternals(HttpRequest request, HttpMethod httpMethod, String url) throws Exception;

	/**
	 * Create default http request used in tests, except test of constructor.
	 *
	 * @return Http request.
	 * @throws Exception
	 */
	protected abstract HttpRequest createDefaultRequest() throws Exception;

	/**
	 * Execute http request using given strategy.
	 * Strategy must execute http request using request
	 * methods.
	 *
	 * @param httpRequest Http request.
	 * @param executionStrategy Execution strategy.
	 * @return Produced http response.
	 * @throws Exception
	 */
	protected abstract HttpResponse fakeExecution(HttpRequest httpRequest, ExecutionStrategy executionStrategy) throws Exception;

	/**
	 * Check that http request get expected query parameter.
	 *
	 * @param httpRequest Http request.
	 * @param name Parameter name.
	 * @param value Parameter value.
	 * @throws Exception
	 */
	protected abstract void checkQueryParam(HttpRequest httpRequest, String name, String value) throws Exception;

	/**
	 * Check that http request get expected header.
	 *
	 * @param httpRequest Http request.
	 * @param name Header name.
	 * @param value Header value.
	 * @throws Exception
	 */
	protected abstract void checkHeader(HttpRequest httpRequest, String name, String value) throws Exception;

	/**
	 * Check that http request execution is valid.
	 *
	 * @param httpResponse Produced http response.
	 * @param headers Headers, if http request should had some headers before execution.
	 * @throws Exception
	 */
	protected abstract void checkExecution(HttpResponse httpResponse, HeaderEntry... headers) throws Exception;

	protected static interface ExecutionStrategy {
		HttpResponse execute(HttpRequest request);
	}

	protected static class HeaderEntry {
		public final String name;

		public final String value;

		public static HeaderEntry header(String name, String value) {
			return new HeaderEntry(name, value);
		}

		public HeaderEntry(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}
