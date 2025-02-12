package qingzhou.engine.util.jnet;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * A dynamic url stream handler factory that stores the current delegate factory
 * in a thread local variable.
 * <p>
 * This allows to change the url handler factory at runtime dynamically through
 * the {@link #push(URLStreamHandlerFactory)} and {@link #pop()} methods.
 */
public class DynamicURLStreamHandlerFactory extends ParentAwareURLStreamHandlerFactory {

    /**
     * The thread local holding the current factory.
     */
    protected static final ThreadLocal FACTORY = new InheritableThreadLocal();

    /**
     * Push a url stream handler factory on top of the stack.
     */
    public static void push(URLStreamHandlerFactory factory) {
        // no need to synchronize as we use a thread local
        if (!(factory instanceof ParentAwareURLStreamHandlerFactory)) {
            factory = new URLStreamHandlerFactoryWrapper(factory);
        }
        URLStreamHandlerFactory old = (URLStreamHandlerFactory) FACTORY.get();
        ((ParentAwareURLStreamHandlerFactory) factory).setParentFactory(old);
        FACTORY.set(factory);
    }

    /**
     * Pop the latest url stream handler factory from the stack.
     */
    public static void pop() {
        ParentAwareURLStreamHandlerFactory factory = (ParentAwareURLStreamHandlerFactory) FACTORY.get();
        if (factory != null) {
            FACTORY.set(factory.getParent());
        }
    }

    /**
     * @see ParentAwareURLStreamHandlerFactory#create(java.lang.String)
     */
    @Override
    protected URLStreamHandler create(String protocol) {
        ParentAwareURLStreamHandlerFactory factory = (ParentAwareURLStreamHandlerFactory) FACTORY.get();
        if (factory != null) {
            return factory.createURLStreamHandler(protocol);
        }
        return null;
    }
}
