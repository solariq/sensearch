package com.expleague.sensearch.web;

import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.web.suggest.Suggester;
import com.google.inject.Injector;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class HK2ToGuiceModule extends AbstractBinder {

  private final Injector guiceInjector;

  public HK2ToGuiceModule(Injector guiceInjector) {
    this.guiceInjector = guiceInjector;
  }

  @Override
  protected void configure() {
    bindFactory(new ServiceFactory<>(guiceInjector, SenSeArch.class)).to(SenSeArch.class);
    bindFactory(new ServiceFactory<>(guiceInjector, Suggester.class)).to(Suggester.class);
    bindFactory(new ServiceFactory<>(guiceInjector, QueryAndResults[].class)).to(QueryAndResults[].class);
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
