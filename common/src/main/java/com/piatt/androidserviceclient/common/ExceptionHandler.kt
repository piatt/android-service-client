package com.piatt.androidserviceclient.common

import android.util.Log

/**
 * Handles uncaught exceptions on all threads
 * in the current process within the runtime of the consuming class
 *
 * @author Benjamin Piatt
 */
class ExceptionHandler(private val codeBlock: () -> Unit) : Thread.UncaughtExceptionHandler {
    /**
     * Stores a reference to Thread's default uncaught exception handler,
     * allowing exceptions to be passed on and handled as usual,
     * after interception and handling in this class is complete
     */
    private val exceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    /**
     * Makes this class Thread's new default exception handler,
     * so that any uncaught exceptions are intercepted here
     */
    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * When an uncaught exception is intercepted,
     * execute the code block that was passed by the consumer,
     * then pass the exception along for normal handling by the runtime
     */
    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {
        Log.e("ExceptionHandler", "EXCEPTION on ${thread?.name}", throwable)
        codeBlock()
        exceptionHandler?.uncaughtException(thread, throwable)
    }
}