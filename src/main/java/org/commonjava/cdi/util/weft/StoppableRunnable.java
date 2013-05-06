package org.commonjava.cdi.util.weft;

import org.commonjava.util.logging.Logger;

public abstract class StoppableRunnable
    implements Runnable
{
    protected final Logger logger = new Logger( getClass() );

    private boolean stop = false;

    private Thread myThread;

    public final synchronized void stop()
    {
        logger.debug( "setting stop flag on %s", this );
        stop = true;
        if ( myThread != null )
        {
            logger.debug( "interrupting current thread: %s for task: %s", myThread, this );
            myThread.interrupt();
        }
    }

    @Override
    public final void run()
    {
        if ( stop )
        {
            logger.debug( "stopping task: %s", this );
            return;
        }

        synchronized ( this )
        {
            myThread = Thread.currentThread();
        }

        doExecute();

        synchronized ( this )
        {
            myThread = null;
        }

        doPostExecute();
    }

    protected abstract void doExecute();

    protected void doPostExecute()
    {

    }

}