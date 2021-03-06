package org.coodex.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by davidoff shen on 2017-03-09.
 */
public abstract class AcceptableServiceSPIFacade<Param_Type, T extends AcceptableService<Param_Type>> extends SPIFacade<T> {

    private final static Logger log = LoggerFactory.getLogger(AcceptableServiceSPIFacade.class);


    public T getServiceInstance(Param_Type param) {
        for (T instance : getAllInstances()) {
            if (instance.accept(param))
                return instance;
        }
        T instance = getDefaultProvider();
        if (instance.accept(param))
            return instance;

        log.warn("no service instance accept this: {}", param);

        return null;
    }
}
