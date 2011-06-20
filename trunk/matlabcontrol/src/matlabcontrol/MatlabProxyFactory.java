package matlabcontrol;

/*
 * Copyright (c) 2011, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Creates instances of {@link MatlabProxy}. Any number of proxies may be created with the factory.
 * <br><br>
 * This class is thread-safe. Proxies may be created simultaneously. While {@link #getProxy()} blocks the calling thread
 * until a proxy is created (or the timeout is reached), any number of threads may call {@code getProxy()} at the same
 * time.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyFactory implements ProxyFactory
{
    private final ProxyFactory _delegateFactory;
    
    /**
     * Constructs the factory using defaults.
     * 
     * @throws MatlabConnectionException 
     */
    public MatlabProxyFactory()
    {
        this(new MatlabProxyFactoryOptions());
    }
    
    /**
     * Constructs the factory with the specified {@code options}. Depending on the whether the factory is running inside
     * MATLAB or outside MATLAB will determine if a given option is used.
     * <br><br>
     * Modifying the options after they have been provided to this constructor will have no effect; they may be modified
     * and used to construct another factory.
     * 
     * @param options
     */
    public MatlabProxyFactory(MatlabProxyFactoryOptions options)
    {
        MatlabProxyFactoryOptions.ImmutableFactoryOptions immutableOptions = options.getImmutableCopy();
                
        if(Configuration.isRunningInsideMatlab())
        {
            _delegateFactory = new LocalMatlabProxyFactory(immutableOptions);
        }
        else
        {
            _delegateFactory = new RemoteMatlabProxyFactory(immutableOptions);
        }
    }

    @Override
    public MatlabProxy getProxy() throws MatlabConnectionException
    {
        return _delegateFactory.getProxy();
    }

    @Override
    public Request requestProxy(RequestCallback callback) throws MatlabConnectionException
    {
        if(callback == null)
        {
            throw new NullPointerException("The request callback may not be null");
        }
        
        return _delegateFactory.requestProxy(callback);
    }
    
    /**
     * Provides the requested proxy.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface RequestCallback
    {
        /**
         * Called when the proxy has been created.
         * 
         * @param proxy 
         */
        public void proxyCreated(MatlabProxy proxy);
    }
    
    /**
     * A request for a {@link MatlabProxy}. Because requests have no timeout, a {@code Request} has no concept of
     * failure.
     * <br><br>
     * This interface is not intended to be implemented by users of matlabcontrol.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface Request
    {
        /**
         * The identifier of the proxy associated with this request. If the proxy is created, then its identifier
         * accessible via {@link MatlabProxy#getIdentifier()} will match the identifier returned by this method.
         * 
         * @return 
         */
        public String getProxyIdentifer();
        
        /**
         * Attempts to cancel the request. If the request has already been completed or cannot successfully be canceled
         * then {@code false} will be returned, otherwise {@code true} will be returned. If the request has already been
         * successfully canceled then this method will have no effect and {@code true} will be returned.
         * 
         * @return if successfully cancelled
         */
        public boolean cancel();
        
        /**
         * If the request has been successfully cancelled.
         * 
         * @return if successfully cancelled
         */
        public boolean isCancelled();
        
        /**
         * Returns {@code true} if the proxy has been created.
         * 
         * @return 
         */
        public boolean isCompleted();
    }
}