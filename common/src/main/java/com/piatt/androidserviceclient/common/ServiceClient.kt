package com.piatt.androidserviceclient.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IInterface
import android.util.Log
import androidx.annotation.CallSuper
import com.piatt.androidserviceclient.common.ServiceClient.State.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.properties.Delegates

/**
 * Allows extenders to create a wrapper for Android services
 * that manages service state, and allows clients to execute code
 * on the managed service interface
 *
 * @author Benjamin Piatt
 */
abstract class ServiceClient<T : IInterface> {
    protected val TAG = ServiceClient::class.java.simpleName

    /**
     * Defines the various states that the service can have,
     * either due to normal work flows or due to unexpected terminations or exceptions.
     * Consumers can utilize these states to react accordingly, or for debugging purposes
     *
     * @property DISCONNECTED: client has not yet connected to service
     * @property CONNECTING: client is in the process of connecting to service
     * @property CONNECTED: client has or is successfully connected to service
     * @property DISCONNECTED_BY_SERVICE: service has or is disconnected from client
     * @property DISCONNECTED_BY_CLIENT: client intentionally has or is disconnected from service
     */
    enum class State {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTED_BY_SERVICE, DISCONNECTED_BY_CLIENT
    }

    /**
     * Current state of the ServiceClient, defaulted to DISCONNECTED
     *
     * When a new assignment is made to the state variable,
     * the observable triggers the state change callback with the new state
     *
     * On a state change, notifies the state callback, if it has been initialized,
     * then resumes all paused code execution coroutines, if service is now connected,
     * so that they can continue their respective code block execution
     *
     * @see State
     */
    protected var state: State by Delegates.observable(DISCONNECTED) {
            _, _, newState -> run {
        when (newState) {
            DISCONNECTED,
            DISCONNECTED_BY_SERVICE,
            DISCONNECTED_BY_CLIENT -> Log.i(TAG, "$newState from $serviceName")
            CONNECTING,
            CONNECTED -> Log.i(TAG, "$newState to $serviceName")
        }
        stateCallback?.onStateChanged(newState)
        if (newState == CONNECTED) {
            Log.i(TAG, "$serviceName now connected! Resuming ${serviceConnectionContinuations.size} execute calls")
            serviceConnectionContinuations.forEach { it.resume(serviceInterface) }
            serviceConnectionContinuations.clear()
        }
    }
    }

    /**
     * Allows implementors to receive state change notifications
     *
     * @see State
     */
    interface StateCallback {
        fun onStateChanged(state: State)
    }

    /**
     * Initialized to null, but can be set by clients
     * if they wish to receive state change notifications,
     * particularly if there is additional logic that should be executed on certain state changes
     *
     * @see StateCallback
     */
    var stateCallback: StateCallback? = null

    /**
     * Class name of the managed Android service,
     * used primarily for logging purposes
     */
    protected abstract val serviceName: String

    /**
     * Managed service interface,
     * representing the implemented stub
     * that can be used to make service calls
     */
    protected var serviceInterface: T? = null

    /**
     * Allows extenders to specify the method
     * by which the service interface is set,
     * either using the binder to return the stub as an interface,
     * or by assigning an implemented stub directly
     */
    protected abstract fun getServiceInterface(service: IBinder? = null): T

    /**
     * Counter to use for each execute call
     * to be logged during execute calls along with the code block description,
     * if one is provided by the client
     */
    private val codeBlockCount = AtomicInteger(0)

    /**
     * Length of time in milliseconds that code execution
     * should wait for service to connect before failing
     */
    private val SERVICE_CONNECTION_TIMEOUT = 10000L

    /**
     * List of cancellable continuations representing paused code execution coroutines
     */
    private var serviceConnectionContinuations = mutableListOf<CancellableContinuation<T?>>()

    /**
     * Allows extenders to execute the given non-returning code block,
     * passing the service interface as an argument
     *
     * @param codeBlockDescription: optional string that may be provided
     * by the client to give more diagnostic insight into ServiceClient runtime
     *
     * @sample: serviceClient.execute {
     *              Log.d(TAG, "Executing non-returning service call")
     *              it.doNonReturningServiceCall()
     *          }
     *
     * @sample: serviceClient.execute("doNonReturningServiceCall()") {
     *              Log.d(TAG, "Executing non-returning service call")
     *              it.doNonReturningServiceCall()
     *          }
     */
    abstract fun execute(codeBlockDescription: String? = null, codeBlock: (T) -> Unit)

    /**
     * Allows extenders to execute the given value-returning code block,
     * passing the service interface as an argument, and requiring a default value
     * that can be returned without blocking the caller, depending on the state flow below:
     *
     * 1) If service is connected and serviceInterface has been initialized,
     * immediately returns the result of executing the code block or the given defaultValue, if the result is null
     * 2) If the service has been intentionally disconnected by the client, immediately returns the given defaultValue
     * 3) If the service is in any other states, assumes that it is not properly connected,
     * and asynchronously returns the result of the following suspendable workflow:
     *
     * Sets the serviceInterface by running the inner suspending block of code inside a coroutine with the given timeout
     * and returns null if the timeout is exceeded. Inside of that block, pauses the coroutine,
     * adds it to the list that will be resumed by the state observable once the service is connected,
     * then attempts to connect to the service if the service is currently disconnected,
     * otherwise notes that service connection is in progress and the coroutine will wait until it is resumed.
     *
     * Once the service connects successfully, the state observable resumes the coroutine with the serviceInterface value,
     * which is then used to return the result of executing the given codeBlock or the given defaultValue, if the result is null
     *
     * If the suspending timeout is exceeded before the service connects
     * or the returned serviceInterface is null, returns the given defaultValue
     *
     * 4) If an exception is caught at any point during the execution of the steps above,
     * notifies the console that it occurred, then immediately returns the given defaultValue
     *
     * Note: This method must be called by a coroutine or another suspending function,
     * allowing extenders to optionally use the coroutine context not to block the caller
     *
     * @param codeBlockDescription: optional string that may be provided
     * by the client to give more diagnostic insight into ServiceClient runtime
     *
     * @sample: val list = runBlocking {
     *                         serviceClient.execute(emptyList()) {
     *                             Log.d(TAG, "Executing value-returning service call")
     *                             it.doValueReturningServiceCall()
     *                         }
     *                     }
     *
     * @sample: val list = async {
     *                         serviceClient.execute(emptyList()) {
     *                             Log.d(TAG, "Executing value-returning service call")
     *                             it.doValueReturningServiceCall()
     *                         }
     *                     }.await()
     *
     * @sample: val list = async {
     *                         serviceClient.execute(emptyList(), "doValueReturningServiceCall()") {
     *                             Log.d(TAG, "Executing value-returning service call")
     *                             it.doValueReturningServiceCall()
     *                         }
     *                     }.await()
     *
     * @see State
     */
    suspend fun <R> execute(defaultValue: R?, codeBlockDescription: String? = null, codeBlock: (T) -> R?): R? {
        val codeBlockCountAndDescription = "code block ${codeBlockCount.getAndIncrement()}${if (!codeBlockDescription.isNullOrBlank()) ": $codeBlockDescription" else ""}"
        return try {
            when {
                state == CONNECTED && serviceInterface != null -> {
                    Log.v(TAG, "$serviceName already connected! Executing $codeBlockCountAndDescription")
                    codeBlock(serviceInterface!!) ?: defaultValue
                }
                state == DISCONNECTED_BY_CLIENT -> {
                    Log.i(TAG, "Did not execute $codeBlockCountAndDescription! Client has disconnected from $serviceName")
                    defaultValue
                }
                else -> withContext(Dispatchers.Default) {
                    val serviceInterface = withTimeoutOrNull<T?>(SERVICE_CONNECTION_TIMEOUT) {
                        suspendCancellableCoroutine {
                            serviceConnectionContinuations.add(it)
                            if ((state == DISCONNECTED || state == DISCONNECTED_BY_SERVICE) && serviceConnectionContinuations.size == 1) {
                                Log.v(TAG, "Connecting to $serviceName before executing $codeBlockCountAndDescription")
                                connectService()
                            } else {
                                Log.v(TAG, "Waiting for $serviceName to connect before executing $codeBlockCountAndDescription")
                            }
                        }
                    }
                    if (state == CONNECTED && serviceInterface != null) {
                        Log.v(TAG, "$serviceName now connected! Executing $codeBlockCountAndDescription")
                        codeBlock(serviceInterface) ?: defaultValue
                    } else {
                        Log.w(TAG, "Failed to execute $codeBlockCountAndDescription! Could not connect to $serviceName")
                        defaultValue
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute $codeBlockCountAndDescription! ${e.message}")
            defaultValue
        }
    }

    /**
     * Extenders must override this method
     * to define how the client should connect to the service
     */
    @CallSuper
    open fun connectService() {
        state = CONNECTING
    }

    /**
     * Extenders must override this method
     * to define how the client should disconnect from the service
     */
    @CallSuper
    open fun disconnectService() {
        state = DISCONNECTED_BY_CLIENT
        serviceInterface = null
    }
}

/**
 * Abstract extender of ServiceClient that wraps and automatically manages
 * the connection between a client and an Android bound service,
 * allowing clients to make service calls without worrying about service state
 *
 * @param context: Android context in which to manage the service
 * @param intent: Android intent defining the desired service to manage
 * @param flags: Android flags defining additional specific bound service behavior
 *
 * @author Benjamin Piatt
 */
abstract class AndroidServiceClient<T : IInterface>(
    private val context: Context,
    private val intent: Intent,
    private val flags: Int = 0,
    override val serviceName: String
) : ServiceClient<T>() {
    /**
     * Receives service connection state callbacks,
     * and notifying connected domains of WeatherClient API availability
     * based on bound service connection state
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            state = DISCONNECTED_BY_SERVICE
            serviceInterface = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceInterface = getServiceInterface(service)
            state = if (serviceInterface != null) CONNECTED else DISCONNECTED
        }
    }

    /**
     * Launches a non-blocking coroutine on the main thread
     * that calls the value-returning version of the execute method,
     * in order to share the same procedure but disregarding the returned value,
     * allowing clients to run code that includes making one-way service calls that do not return a value
     */
    final override fun execute(codeBlockDescription: String?, codeBlock: (T) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { execute(null, codeBlockDescription, codeBlock) }
    }

    /**
     * Executes the superclass version of the method,
     * then binds to the service defined by the intent parameter,
     * assigning the serviceConnection property implemented in this class
     * as the receiver of service state updates
     */
    final override fun connectService() {
        super.connectService()
        context.bindService(intent, serviceConnection, flags)
    }

    /**
     * Executes the superclass version of the method,
     * then unbinds from the service
     */
    final override fun disconnectService() {
        super.disconnectService()
        context.unbindService(serviceConnection)
    }
}

/**
 * Abstract extender of ServiceClient that wraps and mocks the management
 * of the connection between a client and an Android bound service,
 * allowing clients to make service calls without worrying about service state
 * in a way that maintains a linear and testable workflow
 *
 * @author Benjamin Piatt
 */
abstract class TestServiceClient<T : IInterface>(override val serviceName: String) : ServiceClient<T>() {
    /**
     * Runs a blocking coroutine on the calling thread
     * and calls the value-returning version of the execute method,
     * in order to share the same procedure but disregarding the returned value,
     * allowing test clients to run code that includes making one-way service calls that do not return a value
     *
     * Note: Unlike the AndroidServiceClient which has to wait for a connected service
     * before making calls, the TestServiceClient mocks an immediate service connection,
     * and therefore blocks while executing the given codeBlock in order to maintain a linear and testable workflow
     */
    final override fun execute(codeBlockDescription: String?, codeBlock: (T) -> Unit) {
        runBlocking { execute(null, codeBlockDescription, codeBlock) }
    }

    /**
     * Executes the superclass version of the method,
     * then imitates binding to the service by assigning the serviceInterface
     * to the return value of the test client's getServiceInterface implementation
     */
    final override fun connectService() {
        super.connectService()
        serviceInterface = getServiceInterface()
        state = if (serviceInterface != null) CONNECTED else DISCONNECTED
    }
}