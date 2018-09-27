/*
 * Copyright 2015 Sadayuki Furuhashi
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import java.util.List;
import java.util.function.Function;

public class Bootstrap
{
    private final List<Module> modules = Lists.newArrayList();

    private final List<Function<? super List<Module>, ? extends Iterable<? extends Module>>> moduleOverrides = Lists.newArrayList();

    private final List<LifeCycleListener> lifeCycleListeners = Lists.newArrayList();

    private boolean requireExplicitBindings = true;

    private boolean started;

    public Bootstrap(Module... modules)
    {
        this(ImmutableList.copyOf(modules));
    }

    public Bootstrap(Iterable<? extends Module> modules)
    {
        this.modules.addAll(ImmutableList.copyOf(modules));
    }

    public Bootstrap addLifeCycleListeners(LifeCycleListener... listeners)
    {
        return addLifeCycleListeners(ImmutableList.copyOf(listeners));
    }

    public Bootstrap addLifeCycleListeners(Iterable<? extends LifeCycleListener> listeners)
    {
        this.lifeCycleListeners.addAll(ImmutableList.copyOf(listeners));
        return this;
    }

    public Bootstrap requireExplicitBindings(boolean requireExplicitBindings)
    {
        this.requireExplicitBindings = requireExplicitBindings;
        return this;
    }

    public Bootstrap addModules(Module... additionalModules)
    {
        return addModules(ImmutableList.copyOf(additionalModules));
    }

    public Bootstrap addModules(Iterable<? extends Module> additionalModules)
    {
        modules.addAll(ImmutableList.copyOf(additionalModules));
        return this;
    }

    public Bootstrap overrideModulesWith(Module... overridingModules)
    {
        return overrideModulesWith(ImmutableList.copyOf(overridingModules));
    }

    public Bootstrap overrideModulesWith(Iterable<? extends Module> overridingModules)
    {
        final List<Module> immutableCopy = ImmutableList.copyOf(overridingModules);

        return overrideModules(new Function<List<Module>, List<Module>>() {
            public List<Module> apply(List<Module> modules)
            {
                return ImmutableList.of(Modules.override(modules).with(immutableCopy));
            }
        });
    }

    @Deprecated  // Using Guava's Function is deprecated.
    public Bootstrap overrideModules(final com.google.common.base.Function<? super List<Module>, ? extends Iterable<? extends Module>> function)
    {
        final Function<? super List<Module>, ? extends Iterable<? extends Module>> wrapper =
                new Function<List<Module>, Iterable<? extends Module>>() {
                    public Iterable<? extends Module> apply(final List<Module> modules) {
                        return function.apply(modules);
                    }
                };
        moduleOverrides.add(wrapper);
        return this;
    }

    public Bootstrap overrideModules(Function<? super List<Module>, ? extends Iterable<? extends Module>> function)
    {
        moduleOverrides.add(function);
        return this;
    }

    public LifeCycleInjector initialize()
    {
        return build(true);
    }

    public CloseableInjector initializeCloseable()
    {
        return build(false);
    }

    private LifeCycleInjectorProxy build(boolean destroyOnShutdownHook)
    {
        Injector injector = start();
        LifeCycleManager lifeCycleManager = injector.getInstance(LifeCycleManager.class);
        if (destroyOnShutdownHook) {
            lifeCycleManager.destroyOnShutdownHook();
        }
        return new LifeCycleInjectorProxy(injector, lifeCycleManager);
    }

    private Injector start()
    {
        List<Module> userModules = ImmutableList.copyOf(modules);
        for (Function<? super List<Module>, ? extends Iterable<? extends Module>> moduleOverride : moduleOverrides) {
            userModules = ImmutableList.copyOf(moduleOverride.apply(userModules));
        }

        if (started) {
            throw new IllegalStateException("System already initialized");
        }
        started = true;

        ImmutableList.Builder<Module> builder = ImmutableList.builder();

        builder.addAll(userModules);

        builder.add(new Module()
        {
            @Override
            public void configure(Binder binder)
            {
                binder.disableCircularProxies();
                if (requireExplicitBindings) {
                    binder.requireExplicitBindings();
                }
            }
        });

        builder.add(new LifeCycleModule(ImmutableList.copyOf(lifeCycleListeners)));

        Injector injector = Guice.createInjector(Stage.PRODUCTION, builder.build());

        LifeCycleManager lifeCycleManager = injector.getInstance(LifeCycleManager.class);
        if (lifeCycleManager.size() > 0) {
            lifeCycleManager.start();
        }

        return injector;
    }
}
