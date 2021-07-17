package org.reactiveminds.actiongraph.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class ServiceRegistry {
    static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);
    static class ServiceWrapper{
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceWrapper that = (ServiceWrapper) o;
            return Objects.equals(urlPattern, that.urlPattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(urlPattern);
        }

        final UrlPattern urlPattern;
        final HttpService service;

        private ServiceWrapper(UrlPattern urlPattern, HttpService service) {
            this.urlPattern = urlPattern;
            this.service = service;
        }
    }
    private static Set<ServiceWrapper> registry = new HashSet<>();
    private static ConcurrentHashMap<String, ServiceWrapper> matchedPaths = new ConcurrentHashMap<>();

    public static void register(HttpService service){
        boolean add = registry.add(new ServiceWrapper(new UrlPattern(service.pathPattern()), service));
        if(!add)
            throw new IllegalArgumentException("duplicate path pattern: "+service.pathPattern());
        else
            log.info("mapped route: {} {}", service.method(), service.pathPattern());
    }
    public static ServiceWrapper getService(String url){
        if(matchedPaths.containsKey(url))
            return matchedPaths.get(url);
        Optional<ServiceWrapper> optional = registry.stream().filter(serviceWrapper -> serviceWrapper.urlPattern.matches(url)).findFirst();
        if(optional.isPresent()){
            matchedPaths.putIfAbsent(url, optional.get());
            return matchedPaths.get(url);
        }
        return null;
    }
}
