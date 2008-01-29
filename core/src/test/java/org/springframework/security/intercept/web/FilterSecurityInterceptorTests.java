/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.intercept.web;

import junit.framework.TestCase;

import org.springframework.security.AccessDecisionManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.MockAccessDecisionManager;
import org.springframework.security.MockApplicationContext;
import org.springframework.security.MockAuthenticationManager;
import org.springframework.security.MockRunAsManager;
import org.springframework.security.RunAsManager;
import org.springframework.security.SecurityConfig;
import org.springframework.security.util.AntUrlPathMatcher;
import org.springframework.security.util.RegexUrlPathMatcher;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * Tests {@link FilterSecurityInterceptor}.
 *
 * @author Ben Alex
 * @version $Id$
 */
public class FilterSecurityInterceptorTests extends TestCase {
    //~ Constructors ===================================================================================================

    public FilterSecurityInterceptorTests() {
    }

    public FilterSecurityInterceptorTests(String arg0) {
        super(arg0);
    }

    //~ Methods ========================================================================================================

    public final void setUp() throws Exception {
        super.setUp();
        SecurityContextHolder.clearContext();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        SecurityContextHolder.clearContext();
    }

    public void testEnsuresAccessDecisionManagerSupportsFilterInvocationClass() throws Exception {
        FilterSecurityInterceptor interceptor = new FilterSecurityInterceptor();
        interceptor.setAuthenticationManager(new MockAuthenticationManager());
        interceptor.setObjectDefinitionSource(new DefaultFilterInvocationDefinitionSource(new RegexUrlPathMatcher()));
        interceptor.setRunAsManager(new MockRunAsManager());

        interceptor.setAccessDecisionManager(new AccessDecisionManager() {
                public boolean supports(Class clazz) {
                    return false;
                }

                public boolean supports(ConfigAttribute attribute) {
                    return true;
                }

                public void decide(Authentication authentication, Object object, ConfigAttributeDefinition config)
                    throws AccessDeniedException {
                    throw new UnsupportedOperationException("mock method not implemented");
                }
            });

        try {
            interceptor.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("AccessDecisionManager does not support secure object class: class org.springframework.security.intercept.web.FilterInvocation",
                expected.getMessage());
        }
    }

    public void testEnsuresRunAsManagerSupportsFilterInvocationClass()
        throws Exception {
        FilterSecurityInterceptor interceptor = new FilterSecurityInterceptor();
        interceptor.setAccessDecisionManager(new MockAccessDecisionManager());
        interceptor.setAuthenticationManager(new MockAuthenticationManager());
        interceptor.setObjectDefinitionSource(new DefaultFilterInvocationDefinitionSource(new RegexUrlPathMatcher()));

        interceptor.setRunAsManager(new RunAsManager() {
                public boolean supports(Class clazz) {
                    return false;
                }

                public boolean supports(ConfigAttribute attribute) {
                    return true;
                }

                public Authentication buildRunAs(Authentication authentication, Object object,
                    ConfigAttributeDefinition config) {
                    throw new UnsupportedOperationException("mock method not implemented");
                }
            });

        try {
            interceptor.afterPropertiesSet();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("RunAsManager does not support secure object class: class org.springframework.security.intercept.web.FilterInvocation",
                expected.getMessage());
        }
    }

    public void testHttpsInvocationReflectsPortNumber() throws Throwable {
        // Setup the FilterSecurityInterceptor
        FilterSecurityInterceptor interceptor = new FilterSecurityInterceptor();
        interceptor.setAccessDecisionManager(new MockAccessDecisionManager());
        interceptor.setAuthenticationManager(new MockAuthenticationManager());
        interceptor.setRunAsManager(new MockRunAsManager());
        interceptor.setApplicationEventPublisher(MockApplicationContext.getContext());

        // Setup a mock config attribute definition
        ConfigAttributeDefinition def = new ConfigAttributeDefinition();
        def.addConfigAttribute(new SecurityConfig("MOCK_OK"));

        MockFilterInvocationDefinitionMap mockSource = new MockFilterInvocationDefinitionMap("/secure/page.html", def);
        interceptor.setObjectDefinitionSource(mockSource);

        // Setup our expectation that the filter chain will be invoked, as access is granted
        MockFilterChain chain = new MockFilterChain(true);

        // Setup our HTTPS request and response
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/secure/page.html");
        request.setScheme("https");
        request.setServerPort(443);

        // Setup a Context
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("MOCK_OK")});
        SecurityContextHolder.getContext().setAuthentication(token);

        // Create and test our secure object
        FilterInvocation fi = new FilterInvocation(request, response, chain);
        interceptor.invoke(fi);
    }

    public void testNormalStartupAndGetter() throws Exception {
        FilterSecurityInterceptor interceptor = new FilterSecurityInterceptor();
        interceptor.setAccessDecisionManager(new MockAccessDecisionManager());
        interceptor.setAuthenticationManager(new MockAuthenticationManager());

        DefaultFilterInvocationDefinitionSource fidp =
                new DefaultFilterInvocationDefinitionSource(new RegexUrlPathMatcher());
        interceptor.setObjectDefinitionSource(fidp);
        interceptor.setRunAsManager(new MockRunAsManager());
        interceptor.afterPropertiesSet();
        assertTrue(true);
        assertEquals(fidp, interceptor.getObjectDefinitionSource());
    }

    /**
     * We just test invocation works in a success event. There is no need to test  access denied events as the
     * abstract parent enforces that logic, which is extensively tested separately.
     *
     * @throws Throwable DOCUMENT ME!
     */
    public void testSuccessfulInvocation() throws Throwable {
        // Setup the FilterSecurityInterceptor
        FilterSecurityInterceptor interceptor = new FilterSecurityInterceptor();
        interceptor.setAccessDecisionManager(new MockAccessDecisionManager());
        interceptor.setAuthenticationManager(new MockAuthenticationManager());
        interceptor.setRunAsManager(new MockRunAsManager());
        interceptor.setApplicationEventPublisher(MockApplicationContext.getContext());

        // Setup a mock config attribute definition
        ConfigAttributeDefinition def = new ConfigAttributeDefinition();
        def.addConfigAttribute(new SecurityConfig("MOCK_OK"));

        MockFilterInvocationDefinitionMap mockSource = new MockFilterInvocationDefinitionMap("/secure/page.html", def);
        interceptor.setObjectDefinitionSource(mockSource);

        // Setup our expectation that the filter chain will be invoked, as access is granted
        MockFilterChain chain = new MockFilterChain(true);

        // Setup our HTTP request and response
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/secure/page.html");

        // Setup a Context
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("Test", "Password",
                new GrantedAuthority[] {new GrantedAuthorityImpl("MOCK_OK")});
        SecurityContextHolder.getContext().setAuthentication(token);

        // Create and test our secure object
        FilterInvocation fi = new FilterInvocation(request, response, chain);
        interceptor.invoke(fi);
    }

    public void testNotLoadedFromApplicationContext() throws Exception {
        FilterInvocationDefinitionSourceMapping mapping = new FilterInvocationDefinitionSourceMapping();
        mapping.setUrl("/secure/**");
        mapping.addConfigAttribute("ROLE_USER");

        List mappings = new ArrayList(1);
        mappings.add(mapping);

        DefaultFilterInvocationDefinitionSource filterInvocationDefinitionSource
                = new DefaultFilterInvocationDefinitionSource(new AntUrlPathMatcher());
        filterInvocationDefinitionSource.setMappings(mappings);

        FilterSecurityInterceptor filter = new FilterSecurityInterceptor();
        filter.setObjectDefinitionSource(filterInvocationDefinitionSource);

        MockFilterChain filterChain = new MockFilterChain();
        filterChain.expectToProceed = true;

        FilterInvocation fi = new FilterInvocation(
                new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain);
        filter.invoke(fi);
    }

    //~ Inner Classes ==================================================================================================

    private class MockFilterChain implements FilterChain {
        private boolean expectToProceed;

        public MockFilterChain(boolean expectToProceed) {
            this.expectToProceed = expectToProceed;
        }

        private MockFilterChain() {
        }

        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
            if (expectToProceed) {
                assertTrue(true);
            } else {
                fail("Did not expect filter chain to proceed");
            }
        }
    }

    private class MockFilterInvocationDefinitionMap implements FilterInvocationDefinitionSource {
        private ConfigAttributeDefinition toReturn;
        private String servletPath;

        public MockFilterInvocationDefinitionMap(String servletPath, ConfigAttributeDefinition toReturn) {
            this.servletPath = servletPath;
            this.toReturn = toReturn;
        }

        public ConfigAttributeDefinition getAttributes(Object object)
            throws IllegalArgumentException {
            FilterInvocation fi = (FilterInvocation) object;

            if (servletPath.equals(fi.getHttpRequest().getServletPath())) {
                return toReturn;
            } else {
                return null;
            }
        }

        public Iterator getConfigAttributeDefinitions() {
            return null;
        }

        public boolean supports(Class clazz) {
            return true;
        }
    }
}