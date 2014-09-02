try{
    // IO code
} catch (Throwable t){
    if(t instanceof Exception){
        if(t instanceof IOException){
            // handle this exception type
        } else if (t instanceof AnotherExceptionType){
            //handle this one
        } else {
            // We didn't expect this Exception. What could it be? Let's log it, and let it bubble up the hierarchy.
        }
    } else if (t instanceof Error){
        if(t instanceof IOError){
            // handle this Error
        } else if (t instanceof AnotherError){
            //handle different Error
        } else {
            // We didn't expect this Error. What could it be? Let's log it, and let it bubble up the hierarchy.
        }
    } else {
        // This should never be reached, unless you have subclassed Throwable for your own purposes.
        throw t;
    }
}