# Guice-bootstrap

Guice-bootstrap is an extension of [Guice](https://github.com/google/guice) that adds support for JSR 250 Life Cycle annotations.

**@PostConstruct** annotation is used on methods that need to get executed after dependency injection is done to perform any initialization.

**@PreDestroy** annotation is used on methods that are called before the application shuts down.

## PostConstruct

Methods with `@PostConstruct` annotation are called after all depedency injection is done.

```java
import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MyService implements Runnable
{
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void start() {
        executor.submit(() -> { ... });
    }
}
```

## PreDestroy

Methods with `@PreDestroy` annotation are called after the application shuts down.

```java
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class MyService implements Runnable
{
    private final List<File> files = new ArrayList<>();

    @PreDestroy
    public void cleanup() {
        files.stream().forEach(file -> file.delete());
    }
}
```

## Bootstrap

**Bootstrap** is the main class to use guice-bootstrap.

```java
Injector injector = new Bootstrap()
    .addModules(new MyGuiceModule1(), new MyGuiceModule2(), ...)
    .addModules(new MyGuiceModule3(), new MyGuiceModule4(), ...)
    .initialize();
```

### Overriding bindings

**Bootstrap.overrideModulesWith** method allows you to override bindings. This is useful to customize bindings defined by a base class.

```java
// The base class
public class MyService {
    public Bootstrap bootstrap()
    {
        return new Bootstrap()
            .addModules(new MyGuiceModule1(), new MyGuiceModule2(), ...)
            .addModules(new MyGuiceModule3(), new MyGuiceModule4(), ...)
            ;
    }

    public void start()
    {
        Injector injector = bootstrap().initialize();
        ...
    }
}

// Extending class that overrides some bindings
public class MyExtendedService extend MyService {
    @Override
    public Bootstrap bootstrap()
    {
        return super()
            .overrideModulesWith(new MyGuiceModule4(), ...)
            ;
    }
}
```

## CloseableInjector

`Bootstrap.initialize()` sets up a shutdown hook to the Java VM (@Runtime.addShutdownHook`). It ensures that PostDestroy methods are called when Java VM exits even if it's killed by a SIGTERM or Ctrl-C.

But if you want to control the exact timing of shutdown, you can use `Bootstrap.initializeCloseable()` instead. It returns **CloseableInjector** which implements Injector and Closeable interfaces.

```java
Bootstrap bootstrap = new Bootstrap()
    .addModules(...);

try (CloseableInjector injector = bootstrap.initializeCloseable()) {
    ...
}
```

