package com.FoodtimeNeo.catalog.cache;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.*;

@Configuration(proxyBeanMethods = false)
public class CatalogCacheConfiguration {
    /** A single small revision read replaces repeated joins/counts while the catalog is unchanged. */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static DefaultPointcutAdvisor catalogQueryAdvisor(ObjectProvider<JdbcTemplate> jdbc,
            ObjectProvider<PlatformTransactionManager> manager, ObjectProvider<ObjectMapper> json) {
        var cache = new ProjectionCache();
        var pointcut = new StaticMethodMatcherPointcut() {
            @Override public boolean matches(Method method, Class<?> targetClass) {
                return method.isAnnotationPresent(CatalogQuery.class);
            }
        };
        MethodInterceptor interceptor = invocation -> {
          // Device positions and position-dependent ordering are never shared or persisted.
          for (Object argument : invocation.getArguments()) {
            if (argument instanceof com.FoodtimeNeo.search.SearchRequest search
                    && (search.latitude() != null || search.longitude() != null || "distance".equals(search.sort()))) {
                return invocation.proceed();
            }
          }
          var transaction = new TransactionTemplate(manager.getObject());
          transaction.setReadOnly(true);
          transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
          transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
          return transaction.execute(status -> {
            long revision = Objects.requireNonNull(jdbc.getObject().queryForObject(
                    "SELECT revision FROM foodtime.public_catalog_revision WHERE id = 1", Long.class));
            var key = new ArrayList<Object>();
            key.add(invocation.getMethod().toGenericString());
            key.addAll(Arrays.asList(invocation.getArguments()));
            Object hit = cache.get(revision, key);
            if (hit != null) return hit;
            try {
                Object result = invocation.proceed();
                cache.put(revision, key, result, json.getObject().writeValueAsBytes(result).length);
                return result;
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new IllegalStateException("Catalog query failed", failure);
            }
          });
        };
        var advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        // Must wrap Spring's @Transactional advisor (LOWEST_PRECEDENCE) so cache hits and misses share one DB connection.
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        return advisor;
    }

    static final class ProjectionCache {
        private record Entry(Object value, int bytes) { }
        private final LinkedHashMap<List<Object>, Entry> entries = new LinkedHashMap<>(16, .75f, true);
        private long generation = -1;
        private long bytes;
        synchronized Object get(long revision, List<Object> key) {
            advance(revision);
            var entry = revision == generation ? entries.get(key) : null;
            return entry == null ? null : entry.value();
        }
        synchronized void put(long revision, List<Object> key, Object value, int size) {
            advance(revision);
            // An older concurrent database snapshot must never repopulate the current generation.
            if (revision != generation || value == null || size > 2 * 1024 * 1024) return;
            var old = entries.put(Collections.unmodifiableList(new ArrayList<>(key)), new Entry(value, size));
            bytes += size - (old == null ? 0 : old.bytes());
            while (entries.size() > 256 || bytes > 16L * 1024 * 1024) {
                var iterator = entries.entrySet().iterator();
                bytes -= iterator.next().getValue().bytes();
                iterator.remove();
            }
        }
        private void advance(long revision) {
            if (revision > generation) { entries.clear(); bytes = 0; generation = revision; }
        }
    }
}
