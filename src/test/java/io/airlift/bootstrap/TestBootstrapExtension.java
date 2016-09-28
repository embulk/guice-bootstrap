/*
 * Copyright 2016 Sadayuki Furuhashi
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
package org.embulk.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

public class TestBootstrapExtension
{
    public static class Class1
    {
        @Inject
        public Class1() { }
    }

    public static interface Interface1
    {
        int get();
    }

    public static class Implementation1
            implements Interface1
    {
        @Override
        public int get()
        {
            return 1;
        }
    }

    public static class Implementation2
            implements Interface1
    {
        @Override
        public int get()
        {
            return 2;
        }
    }

    @Test
    public void testAddModules()
            throws Exception
    {
        Bootstrap bootstrap = new Bootstrap()
            .addModules(new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(Class1.class);
                }
            })
            .addModules(new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(Interface1.class).to(Implementation1.class);
                }
            });

        Interface1 iface = bootstrap.initialize().getInstance(Interface1.class);
        Assert.assertEquals(iface.get(), 1);
    }

    @Test
    public void testOverrideModules()
            throws Exception
    {
        Bootstrap bootstrap = new Bootstrap()
            .addModules(new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(Interface1.class).to(Implementation1.class);
                }
            })
            .overrideModulesWith(new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(Interface1.class).to(Implementation2.class);
                }
            });

        Interface1 iface = bootstrap.initialize().getInstance(Interface1.class);
        Assert.assertEquals(iface.get(), 2);
    }

    @Test
    public void testOverrideModulesLazily()
            throws Exception
    {
        Bootstrap bootstrap = new Bootstrap()
            .overrideModulesWith(new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(Interface1.class).to(Implementation2.class);
                }
            })
            .addModules(new Module()
            {
                @Override
                public void configure(Binder binder)
                {
                    binder.bind(Interface1.class).to(Implementation1.class);
                }
            });

        Interface1 iface = bootstrap.initialize().getInstance(Interface1.class);
        Assert.assertEquals(iface.get(), 2);
    }
}
