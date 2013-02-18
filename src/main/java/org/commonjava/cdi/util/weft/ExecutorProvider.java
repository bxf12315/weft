/*******************************************************************************
 * Copyright 2013 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.cdi.util.weft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class ExecutorProvider
{

    private final Map<String, ExecutorService> services = new HashMap<String, ExecutorService>();

    private final Logger logger = new Logger( getClass() );

    @PreDestroy
    public void shutdown()
    {
        for ( final Map.Entry<String, ExecutorService> entry : services.entrySet() )
        {
            final ExecutorService service = entry.getValue();

            service.shutdown();
            try
            {
                service.awaitTermination( 1000, TimeUnit.MILLISECONDS );

                if ( !service.isTerminated() )
                {
                    final List<Runnable> running = service.shutdownNow();
                    if ( !running.isEmpty() )
                    {
                        logger.warn( "%d tasks remain for executor: %s", running.size(), entry.getKey() );
                    }
                }
            }
            catch ( final InterruptedException e )
            {
            }
        }
    }

    @Produces
    public ExecutorService getExecutorService( final InjectionPoint ip )
    {
        final ExecutorConfig ec = ip.getAnnotated()
                                    .getAnnotation( ExecutorConfig.class );

        int threadCount = Runtime.getRuntime()
                                 .availableProcessors() * 2;

        boolean daemon = true;

        int priority = 3;

        // TODO: This may cause counter-intuitive sharing of thread pools for un-annotated injections...
        String name = "Unknown";

        if ( ec != null )
        {
            threadCount = ec.threads();
            name = ec.named();
            priority = ec.priority();
            daemon = ec.daemon();
        }

        return getService( name, priority, threadCount, daemon );
    }

    private synchronized ExecutorService getService( final String name, final int priority, final int threadCount,
                                                     final boolean daemon )
    {
        ExecutorService service = services.get( name );
        if ( service == null )
        {
            final ClassLoader ccl = Thread.currentThread()
                                          .getContextClassLoader();

            service = Executors.newFixedThreadPool( threadCount, new ThreadFactory()
            {
                private int counter = 0;

                @Override
                public Thread newThread( final Runnable runnable )
                {
                    final Thread t = new Thread( runnable );
                    t.setContextClassLoader( ccl );
                    t.setName( name + "-" + counter++ );
                    t.setDaemon( daemon );
                    t.setPriority( priority );

                    return t;
                }
            } );

            services.put( name, service );
        }

        return service;
    }

    //    @Produces
    //    public Executor getExecutor( final InjectionPoint ip )
    //    {
    //        return getExecutorService( ip );
    //    }

}
