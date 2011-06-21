package matlabcontrol;

import java.util.concurrent.CopyOnWriteArrayList;

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
 * Allows for Java to communicate with a running MATLAB session. This class cannot be instantiated, instead it can be
 * created by using a {@link MatlabProxyFactory}. The methods used to communicate with MATLAB are defined in the
 * {@link MatlabInteractor} interface which this class implements.
 * <br><br>
 * This proxy is thread-safe. While methods may be called concurrently, they will be completed sequentially on MATLAB's
 * main thread. More than one proxy may be interacting with MATLAB (for instance one proxy might be running inside
 * MATLAB and another might be running outside MATLAB) and their interactions will not occur simultaneously. Regardless
 * of the number of proxies, interaction with MATLAB occurs in a single threaded manner.
 * <br><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>an internal MATLAB exception occurs, typically due to attempting to use a function or variable that does not
 *     exist (frequently due to a typo), or incorrectly calling a function (such as passing the wrong number of
 *     arguments)</li>
 * <li>the proxy has been disconnected via {@link #disconnect()}</li>
 * </ul>
 * <strong>Running outside MATLAB</strong><br>
 * <ul>
 * <li>communication between this JVM and the one that MATLAB is running in is disrupted (likely due to closing
 *     MATLAB)</li>
 * <li>the class of the object to be sent or returned is not {@link java.io.Serializable}</li>
 * <li>the class of the object to be sent or returned is not defined in the JVM receiving the object</li>
 * </ul>
 * <strong>Running inside MATLAB</strong><br>
 * <ul>
 * <li>the method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components
 *     (This is done to prevent MATLAB from hanging indefinitely. To get around this limitation the
 *     {@link matlabcontrol.extensions.MatlabCallbackInteractor} can be used.)</li>
 * </ul>
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public abstract class MatlabProxy implements MatlabInteractor<Object>
{
    /**
     * Unique identifier for this proxy.
     */
    private final Identifier _id;
    
    /**
     * Whether the session of MATLAB this proxy is connected to is an existing session.
     */
    private final boolean _existingSession;
    
    /**
     * Listeners for disconnection.
     */
    private final CopyOnWriteArrayList<DisconnectionListener> _listeners;
    
    /**
     * This constructor is package private to prevent subclasses from outside of this package.
     */
    MatlabProxy(Identifier id, boolean existingSession)
    {
        _id = id;
        _existingSession = existingSession;
        
        _listeners = new CopyOnWriteArrayList<DisconnectionListener>();
    }
    
    /**
     * Returns the unique identifier for this proxy.
     * 
     * @return identifier
     */
    public Identifier getIdentifier()
    {
        return _id;
    }
        
    /**
     * Whether this proxy is connected to a session of MATLAB that was running previous to the request to create this
     * proxy.
     * 
     * @return if existing session
     */
    public boolean isExistingSession()
    {
        return _existingSession;
    }
    
    /**
     * Returns a brief description of this proxy. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() +
                " identifier=" + this.getIdentifier() + ", " +
                " connected=" + this.isConnected() + ", " +
                " existing=" + this.isExistingSession() + 
                "]";
    }
    
    /**
     * Adds a disconnection that will be notified when this proxy becomes disconnected from MATLAB.
     * 
     * @param listener 
     */
    public void addDisconnectionListener(DisconnectionListener listener)
    {
        _listeners.add(listener);
    }

    /**
     * Removes a disconnection listener. It will no longer be notified.
     * 
     * @param listener 
     */
    public void removeDisconnectionListener(DisconnectionListener listener)
    {
        _listeners.remove(listener);
    }
    
    /**
     * Notifies the disconnection listeners this proxy has become disconnected.
     */
    void notifyDisconnectionListeners()
    {
        for(DisconnectionListener listener : _listeners)
        {
            listener.proxyDisconnected(this);
        }
    }
    
    /**
     * Whether this proxy is connected to MATLAB.
     * <br><br>
     * The most likely reasons for this method to return {@code false} is if MATLAB has been closed or it has been
     * disconnected via {@link #disconnect()}.
     * 
     * @return if connected
     */
    public abstract boolean isConnected();
    
    /**
     * Disconnects the proxy from MATLAB. MATLAB will not exit. After disconnecting, any method sent to MATLAB will
     * throw an exception. A proxy cannot be reconnected.
     * 
     * @return the success of disconnecting
     */
    public abstract boolean disconnect();
    
    /**
     * Implementers can be notified when a proxy becomes disconnected from MATLAB.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface DisconnectionListener
    {
        /**
         * Called when the proxy becomes disconnected from MATLAB. The proxy passed in will always be the proxy that
         * the listener was added to. The proxy is provided so that a single implementation of this interface may be
         * used for multiple proxies.
         * 
         * @param proxy disconnected proxy
         */
        public void proxyDisconnected(MatlabProxy proxy);
    }
    
    /**
     * Uniquely identifies a proxy.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface Identifier
    {
        /**
         * Returns {@code true} if {@code other} is an identifier and is equal to this identifier, {@code false}
         * otherwise.
         * 
         * @param other
         * @return 
         */
        @Override
        public boolean equals(Object other);
    }
}