package com.expleague.sensearch.web;

import com.google.inject.Injector;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class HK2ToGuiceModule extends AbstractBinder {

  private Injector guiceInjector;

  public HK2ToGuiceModule(Injector guiceInjector) {
    this.guiceInjector = guiceInjector;
  }

  @Override
  protected void configure() {
    bindFactory(new ServiceFactory<Builder>(guiceInjector, Builder.class)).to(Builder.class);
  }

  private static class ServiceFactory<T> implements Factory<T> {

    private final Injector guiceInjector;

    private final Class<T> serviceClass;

    public ServiceFactory(Injector guiceInjector, Class<T> serviceClass) {

      this.guiceInjector = guiceInjector;
      this.serviceClass = serviceClass;
    }

    @Override
    public T provide() {
      return guiceInjector.getInstance(serviceClass);
    }

    @Override
    public void dispose(T versionResource) {
    }
  }
}